package com.onelogin.oidc.logout

import android.app.Activity
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.onelogin.oidc.Callback
import com.onelogin.oidc.OIDCConfiguration
import com.onelogin.oidc.data.repository.OIDCRepository
import kotlinx.coroutines.channels.consumeEach
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest

internal class SignOutManagerImpl(
    private val configuration: OIDCConfiguration,
    private val repository: OIDCRepository,
    private val signOutFragmentProvider: (EndSessionRequest) -> SignOutFragment
) : SignOutManager {

    override suspend fun signOut(
        idToken: String,
        activity: Activity,
        signOutCallback: Callback<SignOutSuccess, SignOutError>
    ) {
        val authConfiguration = repository.getConfigurations()
        val endSessionRequest = getEndSessionRequest(idToken, configuration, authConfiguration)

        if (activity is FragmentActivity) {
            removeFragmentIfAttached(activity)
            val logoutFragment = signOutFragmentProvider(endSessionRequest)
            attachLogoutFragment(activity, logoutFragment)
            logoutFragment.resultChannel.consumeEach { (response, exception) ->
                if (exception != null) {
                    signOutCallback.onError(SignOutError(exception.message, exception))
                } else if (response != null) {
                    repository.clearAuthState()
                    signOutCallback.onSuccess(SignOutSuccess("Success"))
                }
            }
            removeFragmentIfAttached(activity)
        } else {
            throw IllegalStateException("Your activity should extend FragmentActivity or AppCompatActivity")
        }
    }

    private fun attachLogoutFragment(
        activity: FragmentActivity,
        loginFragment: SignOutFragment
    ) {
        activity.supportFragmentManager.beginTransaction()
            .add(loginFragment, SignOutFragment.LOGOUT_FRAGMENT_TAG)
            .commit()
    }

    private fun removeFragmentIfAttached(activity: FragmentActivity) {
        activity.supportFragmentManager.findFragmentByTag(SignOutFragment.LOGOUT_FRAGMENT_TAG)?.let {
            activity.supportFragmentManager.beginTransaction()
                .remove(it)
                .commit()
        }
    }

    private fun getEndSessionRequest(
        idToken: String,
        configuration: OIDCConfiguration,
        authConfiguration: AuthorizationServiceConfiguration
    ): EndSessionRequest {
        val issuerUrl = Uri.parse(configuration.issuer)

        val endSessionReqBuilder = EndSessionRequest.Builder(authConfiguration)
            .setIdTokenHint(idToken)
            .setPostLogoutRedirectUri(Uri.parse(configuration.redirectUrl))

        return endSessionReqBuilder.build()
    }
}
