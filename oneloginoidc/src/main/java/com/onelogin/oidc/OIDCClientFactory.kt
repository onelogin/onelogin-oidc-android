package com.onelogin.oidc

import android.content.Context
import android.os.Bundle
import com.onelogin.oidc.data.AuthorizationServiceProvider
import com.onelogin.oidc.data.OKHttpProvider
import com.onelogin.oidc.data.network.ConfigurationClient
import com.onelogin.oidc.data.network.NetworkClient
import com.onelogin.oidc.data.repository.OIDCRepositoryImpl
import com.onelogin.oidc.data.stores.OneLoginEncryptionManager
import com.onelogin.oidc.data.stores.OneLoginEncryptionManager.Companion.ONELOGIN_SHARED_PREFERENCES
import com.onelogin.oidc.data.stores.OneLoginStore
import com.onelogin.oidc.login.SignInFragment
import com.onelogin.oidc.login.SignInFragment.Companion.ARG_AUTHORIZATION_REQUEST
import com.onelogin.oidc.login.SignInManagerImpl
import com.onelogin.oidc.logout.SignOutFragment
import com.onelogin.oidc.logout.SignOutManagerImpl

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
        val preferences = context.getSharedPreferences(ONELOGIN_SHARED_PREFERENCES, Context.MODE_PRIVATE)
        val store = OneLoginStore(preferences)
        val repository =
            OIDCRepositoryImpl(configuration, configurationClient, store, encryptionManager)
        AuthorizationServiceProvider.init(context)
        val authorizationService = AuthorizationServiceProvider.authorizationService
        val signInManager = SignInManagerImpl(
            configuration,
            authorizationService,
            repository
        ) { authorizationRequest ->
            val fragment = SignInFragment()
            val args = Bundle()
            args.putString(ARG_AUTHORIZATION_REQUEST, authorizationRequest.jsonSerializeString())
            fragment.arguments = args
            fragment
        }

        val signOutManager = SignOutManagerImpl(
            configuration,
            repository
        ) { endSessionRequest ->
            val fragment = SignOutFragment()
            val args = Bundle()
            args.putString(ARG_AUTHORIZATION_REQUEST, endSessionRequest.jsonSerializeString())
            fragment.arguments = args
            fragment
        }

        return OIDCClientImpl(authorizationService, networkClient, repository, signInManager, signOutManager)
    }
}
