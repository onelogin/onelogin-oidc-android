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
import com.onelogin.oidc.login.SignInError;
import com.onelogin.oidc.login.SignInSuccess;

import static com.onelogin.oidc.appjava.OIDCDemoApp.LOG_TAG;

public class SignInFragment extends Fragment implements View.OnClickListener {

    private OIDCClient oidcClient = OneLoginOIDC.getClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.sign_in_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.sign_in_button) {
            signIn();
        }
    }

    private void signIn() {
        oidcClient.signIn(requireActivity(), new Callback<SignInSuccess, SignInError>() {
            @Override
            public void onSuccess(SignInSuccess signInSuccess) {
                NavDirections action = SignInFragmentDirections.actionSignInFragmentToUserFragment();
                Navigation.findNavController(requireView()).navigate(action);
                Snackbar.make(requireView(), "Signed In", Snackbar.LENGTH_SHORT).show();
            }

            public void onError(@NonNull SignInError signInError) {
                Snackbar.make(requireView(), getString(R.string.error_signing_in, signInError.getMessage()), Snackbar.LENGTH_SHORT)
                        .show();
                Log.d(LOG_TAG, "Error signing in: " + signInError.getMessage(), signInError);
            }
        });
    }
}
