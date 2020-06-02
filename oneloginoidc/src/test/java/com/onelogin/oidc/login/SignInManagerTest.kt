package com.onelogin.oidc.login

import android.app.Activity
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.onelogin.oidc.Callback
import com.onelogin.oidc.OIDCConfiguration
import com.onelogin.oidc.TestRail
import com.onelogin.oidc.data.repository.OIDCRepository
import io.mockk.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.runBlocking
import net.openid.appauth.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignInManagerTest {

    private val authorizationService = mockk<AuthorizationService>()
    private val repository = mockk<OIDCRepository>()
    private val signInFragment = mockk<SignInFragment>()
    private val activity = mockk<FragmentActivity>()
    private val fragmentManager = mockk<FragmentManager>()
    private val fragmentTransaction = mockk<FragmentTransaction>()
    private val callback = mockk<Callback<SignInSuccess, SignInError>>()
    private val spyResponse = spyk(AuthorizationResponse.Builder(mockk()).build())
    private val authState = spyk(AuthState())

    private val oidcConfiguration = OIDCConfiguration.Builder()
        .issuer("testIssuer")
        .scopes(listOf("openid"))
        .redirectUrl("redirectTest")
        .clientId("testClientId")
        .build()

    private lateinit var signInManager: SignInManager

    @Before
    fun setup() {
        signInManager =
            SignInManagerImpl(oidcConfiguration, authorizationService, repository) { _ ->
                signInFragment
            }

        coEvery { repository.getConfigurations() } returns AuthorizationServiceConfiguration(
            Uri.parse("authEndpoint"),
            Uri.parse("tokenEndpoint")
        )
        every { repository.getLatestAuthState() } returns authState
        every { repository.persistAuthState(any()) } returns Unit

        every { authState.update(any<TokenResponse>(), any()) } returns Unit
        every { spyResponse.createTokenExchangeRequest(any()) } returns mockk()

        every { activity.supportFragmentManager } returns fragmentManager
        every { fragmentManager.findFragmentByTag(any()) } returns null
        every { fragmentManager.beginTransaction() } returns fragmentTransaction
        every { fragmentTransaction.add(any<Fragment>(), any()) } answers {
            fragmentTransaction
        }
        every { fragmentTransaction.remove(any()) } returns fragmentTransaction
        every { fragmentTransaction.commit() } returns 1

        every { callback.onSuccess(any()) } returns Unit
        every { callback.onError(any()) } returns Unit
    }

    @Test(expected = IllegalStateException::class)
    @TestRail
    fun signInFailsOnNonFragmentActivity() = runBlocking {
        val activity = mockk<Activity>()
        signInManager.signIn(activity, callback)
    }

    @Test
    @TestRail
    fun signInFlowsExecutesCorrectly() = runBlocking {
        val serviceCallbackSlot = slot<AuthorizationService.TokenResponseCallback>()
        every { signInFragment.resultChannel }.returns(produce<Pair<AuthorizationResponse?, AuthorizationException?>> {
            send(spyResponse to null)
        } as Channel)
        every {
            authorizationService.performTokenRequest(
                any(),
                capture(serviceCallbackSlot)
            )
        } answers {
            serviceCallbackSlot.captured.onTokenRequestCompleted(mockk(), null)
        }

        signInManager.signIn(activity, callback)

        verify(exactly = 2) { repository.persistAuthState(any()) }
        verify { authorizationService.performTokenRequest(any(), any()) }
        verify { callback.onSuccess(any()) }
    }

    @Test
    @TestRail
    fun signInFragmentIsAttachedAndRemoved() = runBlocking {
        val serviceCallbackSlot = slot<AuthorizationService.TokenResponseCallback>()
        every { signInFragment.resultChannel }.returns(produce<Pair<AuthorizationResponse?, AuthorizationException?>> {
            send(spyResponse to null)
        } as Channel)
        every {
            authorizationService.performTokenRequest(
                any(),
                capture(serviceCallbackSlot)
            )
        } answers {
            serviceCallbackSlot.captured.onTokenRequestCompleted(mockk(), null)
        }

        signInManager.signIn(activity, callback)

        verify(exactly = 1) {
            fragmentTransaction.add(
                signInFragment,
                SignInFragment.LOGIN_FRAGMENT_TAG
            )
        }
    }

    @Test
    @TestRail
    fun signInFragmentIsRemovedIfAttatched() = runBlocking {
        val serviceCallbackSlot = slot<AuthorizationService.TokenResponseCallback>()
        every { signInFragment.resultChannel }.returns(produce<Pair<AuthorizationResponse?, AuthorizationException?>> {
            send(spyResponse to null)
        } as Channel)
        every {
            authorizationService.performTokenRequest(
                any(),
                capture(serviceCallbackSlot)
            )
        } answers {
            serviceCallbackSlot.captured.onTokenRequestCompleted(mockk(), null)
        }
        every { fragmentManager.findFragmentByTag(any()) } returns signInFragment

        signInManager.signIn(activity, callback)

        verify(exactly = 2) { fragmentTransaction.remove(signInFragment) }
    }

    @Test
    @TestRail
    fun signInFailsOnAuthorizationException() = runBlocking {
        every { signInFragment.resultChannel }.returns(produce<Pair<AuthorizationResponse?, AuthorizationException?>> {
            send(null to AuthorizationException(1, 1, "error", "testError", null, null))
        } as Channel)
        signInManager.signIn(activity, callback)

        verify { callback.onError(any()) }
    }

    @Test
    @TestRail
    fun signInFragmentFailsIfCodeExchangeFails() = runBlocking {
        val serviceCallbackSlot = slot<AuthorizationService.TokenResponseCallback>()
        every { signInFragment.resultChannel }.returns(produce<Pair<AuthorizationResponse?, AuthorizationException?>> {
            send(spyResponse to null)
        } as Channel)
        every {
            authorizationService.performTokenRequest(
                any(),
                capture(serviceCallbackSlot)
            )
        } answers {
            serviceCallbackSlot.captured.onTokenRequestCompleted(
                null,
                AuthorizationException(1, 1, "error", "testError", null, null)
            )
        }

        signInManager.signIn(activity, callback)

        verify { authorizationService.performTokenRequest(any(), any()) }
        verify { callback.onError(any()) }
    }
}
