package com.onelogin.oidc.appjava;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.onelogin.oidc.OIDCClient;
import com.onelogin.oidc.OneLoginOIDC;
import com.onelogin.oidc.session.SessionInfo;
import com.onelogin.oidc.session.SessionInfoError;

import static com.onelogin.oidc.appjava.OIDCDemoApp.LOG_TAG;

public class MainActivity extends AppCompatActivity {

    private OIDCClient oidcClient = OneLoginOIDC.getClient();
    private View container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container = findViewById(R.id.container);
    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            SessionInfo sessionInfo = oidcClient.getSessionInfo();
            if (sessionInfo.getAuthorized()) {
                showUserScreen();
            }
        } catch (SessionInfoError error) {
            Snackbar.make(container, getString(R.string.error_getting_session_info, error.getMessage()), Snackbar.LENGTH_SHORT)
                    .show();
            Log.d(LOG_TAG, "Error getting user info: " + error.getMessage(), error);
        }
    }

    private void showUserScreen() {
        NavDirections action = SignInFragmentDirections.actionSignInFragmentToUserFragment();
        Navigation.findNavController(this, R.id.nav_host_fragment).navigate(action);
    }
}
