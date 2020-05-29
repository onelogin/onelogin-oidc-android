package com.onelogin.oidc.data.network

import android.net.Uri
import net.openid.appauth.AuthorizationServiceConfiguration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ConfigurationClient {
    suspend fun fetchConfiguration(issuer: String): AuthorizationServiceConfiguration =
        suspendCoroutine { cont ->
            AuthorizationServiceConfiguration
                .fetchFromIssuer(Uri.parse(issuer)) { configuration, ex ->
                    ex?.let {
                        cont.resumeWithException(ex)
                    }
                    configuration?.let {
                        cont.resume(configuration)
                    }
                }
        }
}
