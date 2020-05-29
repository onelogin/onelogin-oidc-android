package com.onelogin.oidc.demo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ViewAnimator
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.onelogin.oidc.Callback
import com.onelogin.oidc.OneLoginOIDC
import com.onelogin.oidc.introspect.IntrospectionError
import com.onelogin.oidc.introspect.TokenIntrospection

class TokenIntrospectionFragment : Fragment() {

    private val active: TextView by lazy { requireView().findViewById<TextView>(R.id.active) }
    private val userId: TextView by lazy { requireView().findViewById<TextView>(R.id.user_id) }
    private val expiration: TextView by lazy { requireView().findViewById<TextView>(R.id.exp) }
    private val issuedAt: TextView by lazy { requireView().findViewById<TextView>(R.id.iat) }
    private val issuer: TextView by lazy { requireView().findViewById<TextView>(R.id.issuer) }
    private val animator: ViewAnimator by lazy { requireView() as ViewAnimator }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_token_introspection, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        animator.displayedChild = 0
        OneLoginOIDC.getClient()
            .introspect(object : Callback<TokenIntrospection, IntrospectionError> {
                override fun onSuccess(success: TokenIntrospection) {
                    animator.displayedChild = 1
                    active.text = if (success.active) "true" else "false"
                    userId.text = success.clientId
                    expiration.text = success.exp
                    issuedAt.text = success.exp
                    issuer.text = success.iss
                }

                override fun onError(error: IntrospectionError) {
                    Snackbar.make(
                        animator,
                        error.localizedMessage,
                        Snackbar.LENGTH_LONG
                    ).show()

                    Log.e(
                        DemoOIDCApp.LOG_TAG,
                        "Error on introspection",
                        error
                    )
                }
            })
    }
}
