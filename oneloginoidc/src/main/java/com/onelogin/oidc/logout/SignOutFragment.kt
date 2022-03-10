package com.onelogin.oidc.logout

import android.content.Intent
import androidx.fragment.app.Fragment
import com.onelogin.oidc.data.AuthorizationServiceProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import net.openid.appauth.AuthorizationException
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.EndSessionResponse
import timber.log.Timber

class SignOutFragment : Fragment() {

    internal val resultChannel = Channel<Pair<EndSessionResponse?, AuthorizationException?>>()

    override fun onResume() {
        super.onResume()
        val authorizationRequestString = arguments?.getString(ARG_AUTHORIZATION_REQUEST)
        val authorizationRequest = authorizationRequestString?.let { EndSessionRequest.jsonDeserialize(authorizationRequestString) }
        authorizationRequest?.let {
            val authIntent = AuthorizationServiceProvider.authorizationService.getEndSessionRequestIntent(it)
            startActivityForResult(authIntent, END_SESSION_REQUEST_CODE)
            arguments?.putString(ARG_AUTHORIZATION_REQUEST, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == END_SESSION_REQUEST_CODE) {
            data?.let {
                val authorizationResponse = EndSessionResponse.fromIntent(data)
                val exception = AuthorizationException.fromIntent(data)
                try {
                    resultChannel.offer(authorizationResponse to exception)
                    resultChannel.close()
                } catch (e: ClosedSendChannelException) {
                    Timber.d("Could not deliver logout result")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resultChannel.close()
    }

    companion object {
        internal const val END_SESSION_REQUEST_CODE = 34001
        internal const val ARG_AUTHORIZATION_REQUEST = "authorization_request"
        internal const val LOGOUT_FRAGMENT_TAG = "logout_fragment"
    }
}
