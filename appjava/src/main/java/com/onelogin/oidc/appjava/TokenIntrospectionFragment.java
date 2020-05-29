package com.onelogin.oidc.appjava;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.onelogin.oidc.Callback;
import com.onelogin.oidc.OneLoginOIDC;
import com.onelogin.oidc.introspect.IntrospectionError;
import com.onelogin.oidc.introspect.TokenIntrospection;

import static com.onelogin.oidc.appjava.OIDCDemoApp.LOG_TAG;

public class TokenIntrospectionFragment extends Fragment {

    private TextView active;
    private TextView userId;
    private TextView expiration;
    private TextView issuedAt;
    private TextView issuer;
    private ViewAnimator animator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_token_introspection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        active = view.findViewById(R.id.active);
        userId = view.findViewById(R.id.user_id);
        expiration = view.findViewById(R.id.exp);
        issuedAt = view.findViewById(R.id.iat);
        issuer = view.findViewById(R.id.issuer);
        animator = (ViewAnimator) view;

        animator.setDisplayedChild(0);
        OneLoginOIDC.getClient().introspect(new Callback<TokenIntrospection, IntrospectionError>() {
            @Override
            public void onSuccess(TokenIntrospection tokenIntrospection) {
                animator.setDisplayedChild(1);
                active.setText(tokenIntrospection.getActive() ? "true" : "false");
                userId.setText(tokenIntrospection.getClientId());
                expiration.setText(tokenIntrospection.getExp());
                issuedAt.setText(tokenIntrospection.getExp());
                issuer.setText(tokenIntrospection.getIss());
            }

            @Override
            public void onError(@NonNull IntrospectionError introspectionError) {
                Snackbar.make(animator, introspectionError.getMessage(), Snackbar.LENGTH_LONG).show();
                Log.e(LOG_TAG, "Error on introspection", introspectionError);
            }
        });

    }
}
