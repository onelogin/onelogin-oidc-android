package com.onelogin.oidc

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.onelogin.oidc.data.network.NetworkClient
import com.onelogin.oidc.data.repository.OIDCRepository
import com.onelogin.oidc.data.repository.OIDCRepositoryImpl
import com.onelogin.oidc.introspect.IntrospectionError
import com.onelogin.oidc.introspect.TokenIntrospection
import com.onelogin.oidc.login.SignInError
import com.onelogin.oidc.login.SignInManager
import com.onelogin.oidc.login.SignInSuccess
import com.onelogin.oidc.refresh.RefreshError
import com.onelogin.oidc.refresh.RefreshSuccess
import com.onelogin.oidc.revoke.RevokeError
import com.onelogin.oidc.revoke.RevokeSuccess
import com.onelogin.oidc.session.SessionInfo
import com.onelogin.oidc.userInfo.UserInfo
import com.onelogin.oidc.userInfo.UserInfoError
import kotlinx.coroutines.*
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenResponse
import timber.log.Timber
import java.lang.ref.WeakReference

internal class OIDCClientImpl(
    private val authorizationService: AuthorizationService,
    private val networkClient: NetworkClient,
    private val repository: OIDCRepository,
    private val signInManager: SignInManager
) : OIDCClient {

    private var currentActivity: WeakReference<Activity>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun signIn(activity: Activity, signInCallback: Callback<SignInSuccess, SignInError>) {
        listenActivityLifecycle(activity)
        activityScope.coroutineContext.cancelChildren()
        activityScope.launch {
            signInManager.signIn(activity, signInCallback)
        }
    }

    override fun revokeToken(revokeTokenCallback: Callback<RevokeSuccess, RevokeError>) {
        repository.getLatestAuthState().let { state ->
            state.performActionWithFreshTokens(authorizationService) { accessToken, _, ex ->
                if (ex != null) {
                    revokeTokenCallback.onError(RevokeError(ex.message, ex))
                } else if (accessToken != null) {
                    scope.launch {
                        runCatching {
                            networkClient.revokeToken(accessToken)
                            repository.clearAuthState()
                        }.fold(
                            { revokeTokenCallback.onSuccess(RevokeSuccess("Success")) },
                            { revokeTokenCallback.onError(RevokeError(it.message, it)) }
                        )
                    }
                }
            }
        }
    }

    override fun refreshToken(refreshTokenCallback: Callback<RefreshSuccess, RefreshError>) {
        val authState = repository.getLatestAuthState()
        val refreshRequest = authState.createTokenRefreshRequest()
        authorizationService.performTokenRequest(refreshRequest) { response: TokenResponse?, exception: AuthorizationException? ->
            if (exception != null) {
                refreshTokenCallback.onError(RefreshError(exception.message, exception))
            } else {
                authState.update(response, exception)
                repository.persistAuthState(authState)
                refreshTokenCallback.onSuccess(RefreshSuccess(SessionInfo.fromAuthState(authState)))
            }
        }
    }

    override fun getUserInfo(getUserInfoCallback: Callback<UserInfo, UserInfoError>) {
        repository.getLatestAuthState().let { state ->
            state.performActionWithFreshTokens(authorizationService) { accessToken, _, ex ->
                if (ex != null) {
                    getUserInfoCallback.onError(UserInfoError(ex.message, ex))
                } else if (accessToken != null) {
                    scope.launch {
                        runCatching {
                            networkClient.getUserInfo(accessToken)
                        }.fold(
                            { getUserInfoCallback.onSuccess(it) },
                            { getUserInfoCallback.onError(UserInfoError(it.message, it)) }
                        )
                    }
                }
            }
        }
    }

    override fun introspect(introspectionCallback: Callback<TokenIntrospection, IntrospectionError>) {
        val weakCallback = WeakReference(introspectionCallback)
        repository.getLatestAuthState().let { state ->
            state.performActionWithFreshTokens(authorizationService) { accessToken, _, ex ->
                if (ex != null) {
                    weakCallback.get()?.onError(IntrospectionError(ex.message, ex))
                } else if (accessToken != null) {
                    scope.launch {
                        runCatching {
                            networkClient.introspectToken(accessToken)
                        }.fold(
                            { weakCallback.get()?.onSuccess(it) },
                            { weakCallback.get()?.onError(IntrospectionError(it.message, it)) }
                        )
                    }
                }
            }
        }
    }

    override fun getSessionInfo(): SessionInfo {
        return try {
            val authState = repository.getLatestAuthState()
            SessionInfo.fromAuthState(authState)
        } catch (exception: OIDCRepositoryImpl.AuthStateNotInitialized) {
            Timber.e("Non active session available")
            SessionInfo(false)
        }
    }

    override fun cancel() {
        scope.coroutineContext.cancelChildren()
    }

    private fun listenActivityLifecycle(activity: Activity) {
        activity.application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity != null && currentActivity?.get() == activity) {
                    activityScope.coroutineContext.cancelChildren()
                    activity.application.unregisterActivityLifecycleCallbacks(this)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityResumed(activity: Activity) {
            }
        })
    }
}
