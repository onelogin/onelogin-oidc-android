package com.onelogin.oidc.demo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.onelogin.oidc.Callback
import com.onelogin.oidc.OneLoginOIDC
import com.onelogin.oidc.logout.SignOutError
import com.onelogin.oidc.logout.SignOutSuccess
import com.onelogin.oidc.refresh.RefreshError
import com.onelogin.oidc.refresh.RefreshSuccess
import com.onelogin.oidc.revoke.RevokeError
import com.onelogin.oidc.revoke.RevokeSuccess

class UserFragment : Fragment(),
    View.OnClickListener {

    private val oidcClient = OneLoginOIDC.getClient()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_user, container, false)


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.log_out).setOnClickListener(this)
        view.findViewById<View>(R.id.refresh_token).setOnClickListener(this)
        view.findViewById<View>(R.id.get_user_info).setOnClickListener(this)
        view.findViewById<View>(R.id.token_introspection).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.log_out -> logout()
            R.id.refresh_token -> refreshToken()
            R.id.get_user_info -> {
                val action =
                    UserFragmentDirections.actionUserFragmentToUserInfoFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
            R.id.token_introspection -> {
                val actionIntrospection =
                    UserFragmentDirections.actionUserFragmentToTokenIntrospectionFragment()
                Navigation.findNavController(requireView()).navigate(actionIntrospection)
            }
        }
    }

    private fun logout() {
        oidcClient.signOut(requireActivity(), object : Callback<SignOutSuccess, SignOutError> {
            override fun onSuccess(success: SignOutSuccess) {
                val action = UserFragmentDirections.actionUserFragmentToSignInFragment()
                Navigation.findNavController(requireView()).navigate(action)
                Snackbar.make(
                    requireView(),
                    "Logged Out",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            override fun onError(error: SignOutError) {
                Snackbar.make(
                    requireView(),
                    getString(R.string.error_logging_out, error.message),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
                Log.d(
                    DemoOIDCApp.LOG_TAG,
                    "Error logging out: ${error.message}",
                    error
                )
            }
        })
    }

    private fun refreshToken() {
        oidcClient.refreshToken(object :
            Callback<RefreshSuccess, RefreshError> {
            override fun onSuccess(success: RefreshSuccess) {
                Snackbar.make(
                    requireView(),
                    "Token refreshed correctly",
                    Snackbar.LENGTH_SHORT
                ).show()
                Log.d(
                    DemoOIDCApp.LOG_TAG,
                    "New token: $success"
                )
            }

            override fun onError(error: RefreshError) {
                Snackbar.make(
                    requireView(),
                    getString(R.string.error_refreshing_token, error.message),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
                Log.d(
                    DemoOIDCApp.LOG_TAG,
                    "Error refreshing token: ${error.message}",
                    error
                )
            }
        })
    }
}
