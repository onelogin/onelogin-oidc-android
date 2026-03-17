package com.onelogin.oidc.login

import android.content.Intent
import androidx.fragment.app.Fragment
import com.onelogin.oidc.data.AuthorizationServiceProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import timber.log.Timber

internal class SignInFragment : Fragment() {

    internal val resultChannel = Channel<Pair<AuthorizationResponse?, AuthorizationException?>>()

    override fun onResume() {
        super.onResume()
        val authorizationRequestString = arguments?.getString(ARG_AUTHORIZATION_REQUEST)
        val authorizationRequest = authorizationRequestString?.let { AuthorizationRequest.jsonDeserialize(authorizationRequestString) }
        authorizationRequest?.let {

            val authIntent = AuthorizationServiceProvider.authorizationService.getAuthorizationRequestIntent(it)
            startActivityForResult(authIntent, AUTHORIZATION_REQUEST_CODE)
            arguments?.putString(ARG_AUTHORIZATION_REQUEST, null)
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
        internal const val ARG_AUTHORIZATION_REQUEST = "authorization_request"
        internal const val LOGIN_FRAGMENT_TAG = "login_fragment"
    }
}
