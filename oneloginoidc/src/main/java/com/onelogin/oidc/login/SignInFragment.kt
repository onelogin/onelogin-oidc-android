package com.onelogin.oidc.login

import android.content.Intent
import androidx.fragment.app.Fragment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import timber.log.Timber


internal class SignInFragment(
    private val authorizationService: AuthorizationService,
    private var authorizationRequest: AuthorizationRequest?
) : Fragment() {

    internal val resultChannel = Channel<Pair<AuthorizationResponse?, AuthorizationException?>>()

    override fun onResume() {
        super.onResume()
        authorizationRequest?.let {
            val authIntent = authorizationService.getAuthorizationRequestIntent(it)
            startActivityForResult(authIntent, AUTHORIZATION_REQUEST_CODE)
            authorizationRequest = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTHORIZATION_REQUEST_CODE) {
            data?.let {
                val authorizationResponse = AuthorizationResponse.fromIntent(data)
                val exception = AuthorizationException.fromIntent(data)
                try {
                    resultChannel.offer(authorizationResponse to exception)
                    resultChannel.close()
                } catch (e: ClosedSendChannelException) {
                    Timber.d("Could not deliver login result")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resultChannel.close()
    }

    companion object {
        internal const val AUTHORIZATION_REQUEST_CODE = 34001
        internal const val LOGIN_FRAGMENT_TAG = "login_fragment"
    }
}
