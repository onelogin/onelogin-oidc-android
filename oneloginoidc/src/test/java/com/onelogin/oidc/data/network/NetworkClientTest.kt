package com.onelogin.oidc.data.network

import androidx.test.core.app.ApplicationProvider
import com.onelogin.oidc.OIDCConfiguration
import com.onelogin.oidc.TestRail
import com.onelogin.oidc.data.OKHttpProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: NetworkClient

    @Before
    fun setup() {
        server = MockWebServer()

        val okHttpClient =
            OKHttpProvider.getOkHttpClient(ApplicationProvider.getApplicationContext())

        val configuration = OIDCConfiguration
            .Builder()
            .issuer(server.url("/oidc/2").toString())
            .clientId("clientId")
            .redirectUrl("redirectUrl")
            .scopes(listOf("openid"))
            .build()

        client = NetworkClient(okHttpClient, configuration)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    @TestRail
    fun logoutRequestIsFormedCorrectly() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        client.logout("token")
        val request = server.takeRequest()

        assertTrue(request.requestUrl.toString().endsWith("oidc/2/logout"))
    }

    @Test
    @TestRail
    fun revokeTokenRequestIsFormedCorrectly() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))
        client.revokeToken("token")
        val request = server.takeRequest()

        assertEquals("token=token&token_type_hint=access_token&client_id=clientId", request.body.readUtf8())
        assertTrue(request.requestUrl.toString().endsWith("oidc/2/token/revocation"))
    }

    @Test
    @TestRail
    fun getUserInfoRequestIsFormedCorrectly() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        client.getUserInfo("token")

        val request = server.takeRequest()

        assertEquals("Bearer token", request.getHeader("Authorization"))
        assertTrue(request.requestUrl.toString().endsWith("oidc/2/me"))
    }

    @Test
    @TestRail
    fun introspectTokenRequestIsFormedCorrectly() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        client.introspectToken("token")

        val request = server.takeRequest()

        assertEquals("token=token&token_type_hint=access_token&client_id=clientId", request.body.readUtf8())
        assertTrue(request.requestUrl.toString().endsWith("oidc/2/token/introspection"))
    }

    @Test(expected = NetworkClient.NetworkException::class)
    @TestRail
    fun errorIsParsedCorrectly() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody(
            "{\"error\":\"sampleError\", \"error_description\":\"sampleErrorDescription\"}"
        ))
        try {
            client.revokeToken("token")
        } catch (exception: Exception) {
            assertEquals("sampleErrorDescription", (exception as NetworkClient.NetworkException).message)
            throw exception
        }
    }
}
