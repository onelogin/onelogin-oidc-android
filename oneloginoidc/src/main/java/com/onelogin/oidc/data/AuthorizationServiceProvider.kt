package com.onelogin.oidc.data

import android.content.Context
import net.openid.appauth.AuthorizationService

object AuthorizationServiceProvider {

    val authorizationService by lazy {
        AuthorizationService(context)
    }

    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context
    }
}
