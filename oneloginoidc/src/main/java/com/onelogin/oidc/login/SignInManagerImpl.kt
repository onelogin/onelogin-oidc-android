package com.onelogin.oidc.login

import android.app.Activity
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.onelogin.oidc.Callback
import com.onelogin.oidc.OIDCConfiguration
import com.onelogin.oidc.data.repository.OIDCRepository
import com.onelogin.oidc.session.SessionInfo
import kotlinx.coroutines.channels.consumeEach
import net.openid.appauth.*

internal class SignInManagerImpl(
    private val configuration: OIDCConfiguration,
    private val authorizationService: AuthorizationService,
    private val repository: OIDCRepository,
    private val signInFragmentProvider: (AuthorizationRequest) -> SignInFragment
) : SignInManager {

    override suspend fun signIn(
        activity: Activity,
        signInCallback: Callback<SignInSuccess, SignInError>
    ) {
        val authConfiguration = repository.getConfigurations()
        val authorizationRequest = getAuthorizationRequest(configuration, authConfiguration)

        if (activity is FragmentActivity) {
            removeFragmentIfAttached(activity)
            val loginFragment = signInFragmentProvider(authorizationRequest)
            attachLoginFragment(activity, loginFragment)
            loginFragment.resultChannel.consumeEach { (response, exception) ->
                if (exception != null) {
                    signInCallback.onError(SignInError(exception.message, exception))
                } else if (response != null) {
                    val state = AuthState(response, exception)
                    repository.persistAuthState(state)
                    exchangeToken(authorizationService, response, signInCallback)
                }
            }
            removeFragmentIfAttached(activity)
        } else {
            throw IllegalStateException("Your activity should extend FragmentActivity or AppCompatActivity")
        }
    }

    private fun exchangeToken(
        authorizationService: AuthorizationService,
        authorizationResponse: AuthorizationResponse,
        signInCallback: Callback<SignInSuccess, SignInError>
    ) {
        authorizationService.performTokenRequest(
            authorizationResponse.createTokenExchangeRequest()
        ) { tokenResponse, authorizationException ->
            if (authorizationException != null) {
                signInCallback.onError(
                    SignInError(
                        authorizationException.message,
                        authorizationException
                    )
                )
            } else if (tokenResponse != null) {
                val state = repository.getLatestAuthState()
                state.update(tokenResponse, authorizationException)
                repository.persistAuthState(state)
                signInCallback.onSuccess(SignInSuccess(SessionInfo.fromAuthState(state)))
            }
        }
    }

    private fun removeFragmentIfAttached(activity: FragmentActivity) {
        activity.supportFragmentManager.findFragmentByTag(SignInFragment.LOGIN_FRAGMENT_TAG)?.let {
            activity.supportFragmentManager.beginTransaction()
                .remove(it)
                .commit()
        }
    }

    private fun attachLoginFragment(
        activity: FragmentActivity,
        loginFragment: SignInFragment
    ) {
        activity.supportFragmentManager.beginTransaction()
            .add(loginFragment, SignInFragment.LOGIN_FRAGMENT_TAG)
            .commit()
    }

    private fun getAuthorizationRequest(
        configuration: OIDCConfiguration,
        authConfiguration: AuthorizationServiceConfiguration
    ) = AuthorizationRequest
        .Builder(
            authConfiguration,
            configuration.clientId,
            ResponseTypeValues.CODE,
            Uri.parse(configuration.redirectUrl)
        )
        .setScope(configuration.scopes.joinToString(" "))
        .build()
}
