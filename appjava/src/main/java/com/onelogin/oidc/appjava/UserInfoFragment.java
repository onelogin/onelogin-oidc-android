package com.onelogin.oidc.appjava;

import android.annotation.SuppressLint;
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
import com.onelogin.oidc.userInfo.UserInfo;
import com.onelogin.oidc.userInfo.UserInfoError;

import static com.onelogin.oidc.appjava.OIDCDemoApp.LOG_TAG;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserInfoFragment extends Fragment {

    private TextView userId;
    private TextView email;
    private TextView preferredName;
    private TextView updatedAt;
    private ViewAnimator animator;
    private final SimpleDateFormat format = new SimpleDateFormat("EEE, MMM d, ''yy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userId = view.findViewById(R.id.user_id);
        email = view.findViewById(R.id.email);
        preferredName = view.findViewById(R.id.username);
        updatedAt = view.findViewById(R.id.updated_at);
        animator = (ViewAnimator) view;

        animator.setDisplayedChild(0);
        OneLoginOIDC.getClient().getUserInfo(new Callback<UserInfo, UserInfoError>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(UserInfo userInfo) {
                userId.setText(userInfo.getSub());
                email.setText(userInfo.getEmail());
                preferredName.setText(userInfo.getPreferredUsername() != null ? userInfo.getPreferredUsername() : "Empty");
                if (userInfo.getUpdatedAt() != null) {
                    updatedAt.setText(format.format(new Date(userInfo.getUpdatedAt() * 1000)));
                } else {
                    updatedAt.setText("Empty");
                }

                animator.setDisplayedChild(1);
            }

            @Override
            public void onError(@NonNull UserInfoError userInfoError) {
                Snackbar.make(requireView(), userInfoError.getMessage(), Snackbar.LENGTH_LONG).show();
                Log.e(LOG_TAG, "Error getting user info", userInfoError);
            }
        });
    }
}
