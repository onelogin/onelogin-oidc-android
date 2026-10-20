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
import net.openid.appauth.AuthorizationServiceDiscovery
import org.json.JSONArray
import org.json.JSONObject
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
        every { store.fetch("issuer") }.returns("testIssuer")
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
    fun testGetConfigurationsSkipsCacheWhenStoreBelongsToAnotherIssuer(): Unit = runBlocking {
        every { store.fetch(any()) }.returns(null)
        every { store.fetch("issuer") }.returns("newerIssuer")
        coEvery { configurationClient.fetchConfiguration(any()) }.returns(dummyIssuerConfiguration)

        val result = repository.getConfigurations()

        verify(exactly = 0) { store.persist(any(), any()) }
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
        every { store.fetch("issuer") }.returns("testIssuer")
        every { encryptionManager.encrypt(any()) }.returns("encrypted")
        every { store.persist(any(), any()) }.returns(true)

        repository.persistAuthState(authState)

        verify { authState.jsonSerializeString() }
        verify { encryptionManager.encrypt("{}") }
        verify { store.persist("authState", "encrypted") }
    }

    @Test
    @TestRail
    fun testPersistAuthStateSkippedWhenStoreBelongsToAnotherIssuer() {
        every { store.fetch("issuer") }.returns("newerIssuer")

        repository.persistAuthState(AuthState())

        verify(exactly = 0) { store.persist(any(), any()) }
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

    @Test
    @TestRail
    fun testKeepsStateWhenStoredIssuerMatches() {
        every { store.fetch("issuer") }.returns("testIssuer")

        repository.discardStateFromOtherIssuer()

        verify(exactly = 0) { store.clear(any()) }
        verify(exactly = 0) { store.persist(any(), any()) }
    }

    @Test
    @TestRail
    fun testDiscardsStateWhenStoredIssuerDiffers() {
        every { store.fetch("issuer") }.returns("previousIssuer")
        every { store.clear(any()) }.returns(Unit)
        every { store.persist(any(), any()) }.returns(true)

        repository.discardStateFromOtherIssuer()

        verify { store.clear("issuer_configurations") }
        verify { store.clear("authState") }
        verify { store.persist("issuer", "testIssuer") }
    }

    @Test
    @TestRail
    fun testKeepsStateWithoutStoredIssuerWhenCachedDiscoveryMatches() {
        val repository = repositoryForIssuer("https://example.onelogin.com/oidc/2")
        every { store.fetch("issuer") }.returns(null)
        every { store.fetch("issuer_configurations") }.returns("encryptedConfig")
        every { encryptionManager.decrypt("encryptedConfig") }
            .returns(discoveredConfiguration("https://example.onelogin.com/oidc/2/").toJsonString())
        every { store.persist(any(), any()) }.returns(true)

        repository.discardStateFromOtherIssuer()

        verify(exactly = 0) { store.clear(any()) }
        verify { store.persist("issuer", "https://example.onelogin.com/oidc/2") }
    }

    @Test
    @TestRail
    fun testDiscardsStateWithoutStoredIssuerWhenCachedDiscoveryDiffers() {
        val repository = repositoryForIssuer("https://example.onelogin.com/oidc/2")
        every { store.fetch("issuer") }.returns(null)
        every { store.fetch("issuer_configurations") }.returns("encryptedConfig")
        every { encryptionManager.decrypt("encryptedConfig") }
            .returns(discoveredConfiguration("https://previous.onelogin.com/oidc/2").toJsonString())
        every { store.clear(any()) }.returns(Unit)
        every { store.persist(any(), any()) }.returns(true)

        repository.discardStateFromOtherIssuer()

        verify { store.clear("issuer_configurations") }
        verify { store.clear("authState") }
        verify { store.persist("issuer", "https://example.onelogin.com/oidc/2") }
    }

    private fun repositoryForIssuer(issuer: String): OIDCRepository {
        val configuration = OIDCConfiguration.Builder()
            .issuer(issuer)
            .clientId("testClientId")
            .redirectUrl("testRedirectUrl")
            .scopes(listOf("openid"))
            .build()
        return OIDCRepositoryImpl(configuration, configurationClient, store, encryptionManager)
    }

    private fun discoveredConfiguration(issuer: String) = AuthorizationServiceConfiguration(
        AuthorizationServiceDiscovery(
            JSONObject()
                .put("issuer", issuer)
                .put("authorization_endpoint", "$issuer/auth")
                .put("token_endpoint", "$issuer/token")
                .put("jwks_uri", "$issuer/certs")
                .put("response_types_supported", JSONArray().put("code"))
                .put("subject_types_supported", JSONArray().put("public"))
                .put("id_token_signing_alg_values_supported", JSONArray().put("RS256"))
        )
    )
}
