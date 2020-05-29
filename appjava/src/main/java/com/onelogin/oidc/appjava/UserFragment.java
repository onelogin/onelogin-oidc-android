package com.onelogin.oidc.appjava;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;
import com.onelogin.oidc.Callback;
import com.onelogin.oidc.OIDCClient;
import com.onelogin.oidc.OneLoginOIDC;
import com.onelogin.oidc.refresh.RefreshError;
import com.onelogin.oidc.refresh.RefreshSuccess;
import com.onelogin.oidc.revoke.RevokeError;
import com.onelogin.oidc.revoke.RevokeSuccess;

import static com.onelogin.oidc.appjava.OIDCDemoApp.LOG_TAG;

public class UserFragment extends Fragment implements View.OnClickListener {

    private OIDCClient oidcClient = OneLoginOIDC.getClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.log_out).setOnClickListener(this);
        view.findViewById(R.id.refresh_token).setOnClickListener(this);
        view.findViewById(R.id.get_user_info).setOnClickListener(this);
        view.findViewById(R.id.token_introspection).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.log_out:
                logout();
                break;
            case R.id.refresh_token:
                refreshToken();
                break;
            case R.id.get_user_info:
                NavDirections action = UserFragmentDirections.actionUserFragmentToUserInfoFragment();
                Navigation.findNavController(requireView()).navigate(action);
                break;
            case R.id.token_introspection:
                NavDirections actionIntrospection = UserFragmentDirections.actionUserFragmentToTokenIntrospectionFragment();
                Navigation.findNavController(requireView()).navigate(actionIntrospection);
                break;
        }
    }

    private void logout() {
        oidcClient.revokeToken(new Callback<RevokeSuccess, RevokeError>() {
            @Override
            public void onSuccess(RevokeSuccess revokeSuccess) {
                NavDirections action = UserFragmentDirections.actionUserFragmentToSignInFragment();
                Navigation.findNavController(requireView()).navigate(action);
                Snackbar.make(requireView(), "Logged Out", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull RevokeError revokeError) {
                Snackbar.make(requireView(), getString(R.string.error_logging_out, revokeError.getMessage()), Snackbar.LENGTH_SHORT)
                        .show();
                Log.d(LOG_TAG, "Error logging out: " + revokeError.getMessage(), revokeError);
            }
        });
    }

    private void refreshToken() {
        oidcClient.refreshToken(new Callback<RefreshSuccess, RefreshError>() {
            @Override
            public void onSuccess(RefreshSuccess refreshSuccess) {
                Snackbar.make(requireView(), "Token refreshed correctly", Snackbar.LENGTH_SHORT).show();
                Log.d(LOG_TAG, "New token: " + refreshSuccess.toString());
            }

            @Override
            public void onError(@NonNull RefreshError refreshError) {
                Snackbar.make(requireView(), getString(R.string.error_refreshing_token, refreshError.getMessage()), Snackbar.LENGTH_SHORT)
                        .show();
                Log.d(LOG_TAG, "Error refreshing token: " + refreshError.getMessage(), refreshError);
            }
        });
    }

}
