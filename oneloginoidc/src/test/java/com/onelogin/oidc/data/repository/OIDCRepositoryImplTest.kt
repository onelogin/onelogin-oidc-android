package com.onelogin.oidc.data.repository

import android.net.Uri
import com.onelogin.oidc.OIDCConfiguration
import com.onelogin.oidc.TestRail
import com.onelogin.oidc.data.network.ConfigurationClient
import com.onelogin.oidc.data.stores.EncryptionManager
import com.onelogin.oidc.data.stores.Store
import io.mockk.*
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OIDCRepositoryImplTest {

    private val oidcCConfiguration = OIDCConfiguration.Builder()
        .issuer("testIssuer")
        .clientId("testClientId")
        .redirectUrl("testRedirectUrl")
        .scopes(listOf("openid"))
        .build()

    private val configurationClient = mockk<ConfigurationClient>()
    private val store = mockk<Store>()
    private val encryptionManager = mockk<EncryptionManager>()

    private val dummyIssuerConfiguration =
        AuthorizationServiceConfiguration(Uri.parse("test"), Uri.parse("test"))
    private lateinit var repository: OIDCRepository

    @Before
    fun setup() {
        repository =
            OIDCRepositoryImpl(oidcCConfiguration, configurationClient, store, encryptionManager)
    }

    @Test
    @TestRail
    fun testGetConfigurationsPullsFromNetworkAndStoresWhenNoLocalValue(): Unit = runBlocking {
        every { store.fetch(any()) }.returns(null)
        every { store.persist(any(), any()) }.returns(true)
        every { encryptionManager.encrypt(any()) }.returns("encrypted")
        coEvery { configurationClient.fetchConfiguration(any()) }.returns(dummyIssuerConfiguration)

        val result = repository.getConfigurations()

        verify(exactly = 1) { store.fetch("issuer_configurations") }
        coVerify(exactly = 1) { configurationClient.fetchConfiguration("testIssuer") }
        verify(exactly = 1) { encryptionManager.encrypt(dummyIssuerConfiguration.toJsonString()) }
        verify(exactly = 1) { store.persist("issuer_configurations", "encrypted") }
        assertEquals(dummyIssuerConfiguration, result)
    }

    @Test
    @TestRail
    fun testPullConfigurationFromLocalStorageWhenItExists() = runBlocking {
        every { store.fetch(any()) }.returns("encryptedData")
        every { encryptionManager.decrypt(any()) }.returns(dummyIssuerConfiguration.toJsonString())

        val result = repository.getConfigurations()

        verify(exactly = 1) { store.fetch("issuer_configurations") }
        verify(exactly = 1) { encryptionManager.decrypt("encryptedData") }
        assertEquals(dummyIssuerConfiguration.authorizationEndpoint, result.authorizationEndpoint)
        assertEquals(dummyIssuerConfiguration.tokenEndpoint, result.tokenEndpoint)
    }

    @Test
    @TestRail
    fun testPersistAuthState() {
        val authState = spyk(AuthState())
        every { encryptionManager.encrypt(any()) }.returns("encrypted")
        every { store.persist(any(), any()) }.returns(true)

        repository.persistAuthState(authState)

        verify { authState.jsonSerializeString() }
        verify { encryptionManager.encrypt("{}") }
        verify { store.persist("authState", "encrypted") }
    }

    @Test
    @TestRail
    fun testGetLatestAuthState() {
        every { store.fetch(any()) }.returns("data")
        every { encryptionManager.decrypt(any()) }.returns("{}")

        val result = repository.getLatestAuthState()

        verify { store.fetch("authState") }
        verify { encryptionManager.decrypt("data") }
        assertFalse(result.isAuthorized)
    }

    @Test
    @TestRail
    fun testClearAuthState() {
        every { store.clear(any()) }.returns(Unit)

        repository.clearAuthState()

        verify { store.clear("authState") }
    }
}
