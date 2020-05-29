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
    }

    class AuthStateNotInitialized : IllegalStateException()
}
