package com.onelogin.oidc.data.repository

import com.onelogin.oidc.OIDCConfiguration
import com.onelogin.oidc.data.network.ConfigurationClient
import com.onelogin.oidc.data.stores.EncryptionManager
import com.onelogin.oidc.data.stores.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration

internal class OIDCRepositoryImpl(
    private val configuration: OIDCConfiguration,
    private val configurationClient: ConfigurationClient,
    private val store: Store,
    private val encryptionManager: EncryptionManager
) : OIDCRepository {

    override suspend fun getConfigurations(): AuthorizationServiceConfiguration =
        withContext(Dispatchers.IO) {
            store.fetch(CONFIGURATIONS_KEY)?.let {
                val decryptedJson = encryptionManager.decrypt(it)
                AuthorizationServiceConfiguration.fromJson(decryptedJson)
            } ?: fetchConfigurationAndStore()
        }

    override fun persistAuthState(authState: AuthState) {
        val serializedState = authState.jsonSerializeString()
        val encryptedState = encryptionManager.encrypt(serializedState)
        store.persist(AUTH_STATE_KEY, encryptedState)
    }

    override fun getLatestAuthState(): AuthState {
        return store.fetch(AUTH_STATE_KEY)?.let {
            val decryptedState = encryptionManager.decrypt(it)
            AuthState.jsonDeserialize(decryptedState)
        } ?: throw AuthStateNotInitialized()
    }

    override fun clearAuthState() {
        store.clear(AUTH_STATE_KEY)
    }

    /**
     * The cached discovery document and the auth state both point at the issuer they came from, and
     * tokens are refreshed against that issuer's endpoints. If the app is now configured with a
     * different issuer, keeping them would keep issuing tokens the new issuer rejects, so drop both.
     */
    override fun discardStateFromOtherIssuer() {
        val storedIssuer = store.fetch(ISSUER_KEY)
        if (storedIssuer == configuration.issuer) return

        val previousIssuer = storedIssuer ?: cachedDiscoveryIssuer()
        if (previousIssuer?.trimEnd('/') != configuration.issuer.trimEnd('/')) {
            store.clear(CONFIGURATIONS_KEY)
            store.clear(AUTH_STATE_KEY)
        }
        store.persist(ISSUER_KEY, configuration.issuer)
    }

    // Stores written before the issuer was recorded only have the cached discovery document to go on
    private fun cachedDiscoveryIssuer(): String? = runCatching {
        store.fetch(CONFIGURATIONS_KEY)?.let {
            val decryptedJson = encryptionManager.decrypt(it)
            AuthorizationServiceConfiguration.fromJson(decryptedJson).discoveryDoc?.issuer
        }
    }.getOrNull()

    private suspend fun fetchConfigurationAndStore(): AuthorizationServiceConfiguration {
        val configuration = configurationClient.fetchConfiguration(configuration.issuer)
        val serialized = configuration.toJsonString()
        val encrypted = encryptionManager.encrypt(serialized)
        store.persist(CONFIGURATIONS_KEY, encrypted)
        return configuration
    }

    companion object {
        private const val CONFIGURATIONS_KEY = "issuer_configurations"
        private const val AUTH_STATE_KEY = "authState"
        private const val ISSUER_KEY = "issuer"
    }

    class AuthStateNotInitialized : IllegalStateException()
}
