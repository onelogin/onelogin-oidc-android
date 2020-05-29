package com.onelogin.oidc

import android.content.Context
import timber.log.Timber

object OneLoginOIDC {

    private var client: OIDCClient? = null

    @JvmStatic
    fun initialize(context: Context, configuration: OIDCConfiguration) {
        if (configuration.debug) {
            Timber.plant(Timber.DebugTree())
        }
        client = OIDCClientFactory(context, configuration).build()
    }

    @JvmStatic
    fun getClient(): OIDCClient = requireNotNull(client) {
        "You should call OneLoginOIDC.initialize() before accessing the client"
    }
}
