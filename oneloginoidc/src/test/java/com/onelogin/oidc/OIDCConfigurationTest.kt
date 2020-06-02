package com.onelogin.oidc

import org.junit.Assert.assertEquals
import org.junit.Test

class OIDCConfigurationTest {

    @Test(expected = IllegalArgumentException::class)
    @TestRail
    fun testRequiredIssuer() {
        OIDCConfiguration.Builder()
            .clientId("clientId")
            .redirectUrl("redirectUrl")
            .scopes(listOf("oidc"))
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    @TestRail
    fun testRequiredClientId() {
        OIDCConfiguration.Builder()
            .issuer("http://issuer.com")
            .redirectUrl("redirectUrl")
            .scopes(listOf("oidc"))
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    @TestRail
    fun testRequiredRedirectUrl() {
        OIDCConfiguration.Builder()
            .issuer("http://issuer.com")
            .clientId("clientId")
            .scopes(listOf("oidc"))
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    @TestRail
    fun testRequiredScopes() {
        OIDCConfiguration.Builder()
            .issuer("http://issuer.com")
            .clientId("clientId")
            .redirectUrl("redirectUrl")
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    @TestRail
    fun testRequiredOpenIdScope() {
        OIDCConfiguration.Builder()
            .issuer("http://issuer.com")
            .clientId("clientId")
            .redirectUrl("redirectUrl")
            .scopes(listOf("nonopenid"))
            .build()
    }

    @Test
    @TestRail
    fun testConfigurationIsBuildCorrectly() {
        val configuration = OIDCConfiguration.Builder()
            .issuer("http://issuer.com")
            .clientId("clientId")
            .redirectUrl("redirectUrl")
            .scopes(listOf("openid"))
            .build()

        assertEquals("http://issuer.com", configuration.issuer)
        assertEquals("clientId", configuration.clientId)
        assertEquals("redirectUrl", configuration.redirectUrl)
        assertEquals(listOf("openid"), configuration.scopes)
    }
}
