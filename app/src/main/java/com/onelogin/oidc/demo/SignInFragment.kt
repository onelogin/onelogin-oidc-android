package com.onelogin.oidc.demo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.onelogin.oidc.Callback
import com.onelogin.oidc.OneLoginOIDC
import com.onelogin.oidc.login.SignInError
import com.onelogin.oidc.login.SignInSuccess

class SignInFragment : Fragment(), View.OnClickListener {

    private val oidcClient = OneLoginOIDC.getClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_sign_in, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.sign_in_button).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.sign_in_button) {
            signIn()
        }
    }

    private fun signIn() {
        oidcClient.signIn(
            requireActivity(), object : Callback<SignInSuccess, SignInError> {
                override fun onSuccess(success: SignInSuccess) {
                    val action = SignInFragmentDirections.actionSignInFragmentToUserFragment()
                    Navigation.findNavController(requireView()).navigate(action)
                    Snackbar.make(requireView(), "Signed In", Snackbar.LENGTH_SHORT).show()
                }

                override fun onError(loginError: SignInError) {
                    Snackbar
                        .make(
                            requireView(),
                            getString(R.string.error_signing_in, loginError.message),
                            Snackbar.LENGTH_SHORT
                        )
                        .show()

                    Log.d(
                        DemoOIDCApp.LOG_TAG,
                        "Error signing in: " + loginError.message,
                        loginError
                    )
                }
            })
    }
}
