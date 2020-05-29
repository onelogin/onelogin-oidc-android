package com.onelogin.oidc.session

import net.openid.appauth.AuthState

/**
 * Object containing the information of the latest Session info
 *
 * @param authorized Whether or not there is a valid session open
 * @param accessToken Latest retrieved Access Token
 * @param expirationTime Time of when accessToken will expire in milliseconds since the UNIX Epoc
 * @param refreshToken Refresh Token for the current session
 */
data class SessionInfo(
    val authorized: Boolean,
    val accessToken: String? = null,
    val expirationTime: Long? = null,
    val idToken: String? = null,
    val refreshToken: String? = null
) {

    companion object {
        @JvmStatic
        fun fromAuthState(authState: AuthState): SessionInfo {
            return SessionInfo(
                authState.isAuthorized,
                authState.accessToken,
                authState.accessTokenExpirationTime,
                authState.idToken,
                authState.refreshToken
            )
        }
    }
}
