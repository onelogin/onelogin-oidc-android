package com.onelogin.oidc.demo

import android.app.Application
import com.onelogin.oidc.OIDCConfiguration
import com.onelogin.oidc.OneLoginOIDC

class DemoOIDCApp : Application() {

    override fun onCreate() {
        super.onCreate()
        OneLoginOIDC.initialize(
            this, OIDCConfiguration.Builder()
                .clientId(BuildConfig.CLIENT_ID)
                .issuer(BuildConfig.ISSUER)
                .redirectUrl(BuildConfig.REDIRECT_URL)
                .scopes(listOf("openid"))
                .loginHint("test@email.com")
                .isDebug(true)
                .build()
        )
    }

    companion object {
        const val LOG_TAG = "oidcDemo"
    }
}
