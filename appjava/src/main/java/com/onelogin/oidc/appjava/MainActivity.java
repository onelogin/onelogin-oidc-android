package com.onelogin.oidc.appjava;

import android.os.*;
import android.util.*;
import android.view.*;

import androidx.appcompat.app.*;
import androidx.navigation.*;

import com.google.android.material.snackbar.*;
import com.onelogin.oidc.*;
import com.onelogin.oidc.session.*;

import static com.onelogin.oidc.appjava.OIDCDemoApp.*;

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
        if(getNavController().getCurrentDestination().getId() == R.id.signInFragment) {
            NavDirections action = SignInFragmentDirections.actionSignInFragmentToUserFragment();
            getNavController().navigate(action);
        }
    }

    private NavController getNavController () {
        return Navigation.findNavController(this, R.id.nav_host_fragment);
    }
}
