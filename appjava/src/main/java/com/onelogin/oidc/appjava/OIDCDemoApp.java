package com.onelogin.oidc.appjava;

import android.app.Application;

import com.onelogin.oidc.OIDCConfiguration;
import com.onelogin.oidc.OneLoginOIDC;

import java.util.ArrayList;
import java.util.List;

public class OIDCDemoApp extends Application {

    static final String LOG_TAG = "OneloginOIDC";

    @Override
    public void onCreate() {
        super.onCreate();

        List<String> scopes = new ArrayList<>();
        scopes.add("openid");

        OIDCConfiguration configuration = new OIDCConfiguration.Builder()
                .clientId(BuildConfig.CLIENT_ID)
                .issuer(BuildConfig.ISSUER)
                .redirectUrl(BuildConfig.REDIRECT_URL)
                .scopes(scopes)
                .isDebug(true)
                .build();

        OneLoginOIDC.initialize(this, configuration);
    }
}
