package com.onelogin.oidc.data.repository

import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration

internal interface OIDCRepository {
    suspend fun getConfigurations(): AuthorizationServiceConfiguration
    fun persistAuthState(authState: AuthState)
    fun getLatestAuthState(): AuthState
    fun clearAuthState()
}
