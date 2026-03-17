package com.onelogin.oidc.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.onelogin.oidc.OneLoginOIDC
import com.onelogin.oidc.demo.DemoOIDCApp.Companion.LOG_TAG
import com.onelogin.oidc.session.SessionInfoError
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val oidcClient = OneLoginOIDC.getClient()
    private val navController: NavController
        get() = Navigation.findNavController(
            this,
            R.id.nav_host_fragment
        )


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
                BaseTransientBottomBar.LENGTH_SHORT
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
        if (navController.currentDestination?.id == R.id.signInFragment) {
            val action = SignInFragmentDirections.actionSignInFragmentToUserFragment()
            navController.navigate(action)
        }
    }
}
