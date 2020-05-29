package com.onelogin.oidc

import android.content.Context
import com.onelogin.oidc.data.OKHttpProvider
import com.onelogin.oidc.data.network.ConfigurationClient
import com.onelogin.oidc.data.network.NetworkClient
import com.onelogin.oidc.data.repository.OIDCRepositoryImpl
import com.onelogin.oidc.data.stores.OneLoginEncryptionManager
import com.onelogin.oidc.data.stores.OneLoginStore
import com.onelogin.oidc.login.SignInFragment
import com.onelogin.oidc.login.SignInManagerImpl
import net.openid.appauth.AuthorizationService

internal class OIDCClientFactory(
    private val context: Context,
    private val configuration: OIDCConfiguration
) {

    fun build(): OIDCClient {
        val okHttpClient = OKHttpProvider.getOkHttpClient(context)
        val configurationClient = ConfigurationClient()
        val encryptionManager =
            configuration.encryptionManager ?: OneLoginEncryptionManager(context)
        val networkClient = NetworkClient(okHttpClient, configuration)
        val preferences = context.getSharedPreferences("oneloginPreferences", Context.MODE_PRIVATE)
        val store = OneLoginStore(preferences)
        val repository =
            OIDCRepositoryImpl(configuration, configurationClient, store, encryptionManager)
        val authorizationService = AuthorizationService(context)
        val signInManager = SignInManagerImpl(
            configuration,
            authorizationService,
            repository
        ) { service, authorizationRequest ->
            SignInFragment(service, authorizationRequest)
        }

        return OIDCClientImpl(authorizationService, networkClient, repository, signInManager)
    }

}
