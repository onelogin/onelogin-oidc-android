package com.onelogin.oidc.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.onelogin.oidc.OneLoginOIDC
import com.onelogin.oidc.demo.DemoOIDCApp.Companion.LOG_TAG
import com.onelogin.oidc.session.SessionInfoError
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val oidcClient = OneLoginOIDC.getClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        try {
            val (authorized) = oidcClient.getSessionInfo()
            if (authorized) {
                showUserScreen()

            }
        } catch (error: SessionInfoError) {
            Snackbar.make(
                container,
                getString(R.string.error_getting_session_info, error.message),
                Snackbar.LENGTH_SHORT
            )
                .show()
            Log.d(
                LOG_TAG,
                "Error getting user info: " + error.message,
                error
            )
        }
    }

    private fun showUserScreen() {
        val action = SignInFragmentDirections.actionSignInFragmentToUserFragment()
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(action)
    }
}
