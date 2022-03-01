package com.onelogin.oidc.data.network

import com.google.gson.Gson
import com.onelogin.oidc.OIDCConfiguration
import com.onelogin.oidc.data.models.ErrorResponse
import com.onelogin.oidc.introspect.TokenIntrospection
import com.onelogin.oidc.userInfo.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

internal class NetworkClient(
    private val okHttpClient: OkHttpClient,
    private val configuration: OIDCConfiguration
) {

    suspend fun logout(token: String) = withContext(Dispatchers.IO) {
        val url = "${configuration.issuer}${EndpointsContract.LOGOUT_PATH}?id_token_hint=$token"

        val request = Request.Builder()
            .url(url)
            .build()

        val response = okHttpClient.newCall(request).execute()

        response.use {
            if (response.code == 302) {
                return@withContext
            }
            response.body?.let {
                val error = Gson().fromJson(it.string(), ErrorResponse::class.java)
                throw NetworkException(error.errorDescription)
            } ?: throw NetworkException("Unable to sign out token")
        }
    }

    suspend fun revokeToken(token: String) = withContext(Dispatchers.IO) {
        val requestBody = FormBody.Builder()
            .add("token", token)
            .add("token_type_hint", "access_token")
            .add("client_id", configuration.clientId)
            .build()

        val request = Request.Builder()
            .url(configuration.issuer + EndpointsContract.REVOKE_PATH)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()

        response.use {
            if (response.isSuccessful) {
                return@withContext
            }
            response.body?.let {
                val error = Gson().fromJson(it.string(), ErrorResponse::class.java)
                throw NetworkException(error.errorDescription)
            } ?: throw NetworkException("Unable to revoke token")
        }
    }

    suspend fun getUserInfo(token: String): UserInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(configuration.issuer + EndpointsContract.USER_INFO_PATH)
            .header("Authorization", "Bearer $token")
            .build()

        val response = okHttpClient.newCall(request).execute()

        return@withContext response.use {
            if (response.isSuccessful && response.body != null) {
                Gson().fromJson(response.body!!.string(), UserInfo::class.java)
            } else {
                response.body?.let {
                    val error = Gson().fromJson(it.string(), ErrorResponse::class.java)
                    throw NetworkException(error.errorDescription)
                } ?: throw NetworkException("Unable to get user info")
            }
        }
    }

    suspend fun introspectToken(token: String): TokenIntrospection = withContext(Dispatchers.IO) {
        val requestBody = FormBody.Builder()
            .add("token", token)
            .add("token_type_hint", "access_token")
            .add("client_id", configuration.clientId)
            .build()

        val request = Request.Builder()
            .url(configuration.issuer + EndpointsContract.INTROSPECTION_PATH)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()

        return@withContext response.use {
            if (response.isSuccessful && response.body != null) {
                Gson().fromJson(response.body!!.string(), TokenIntrospection::class.java)
            } else {
                response.body?.let {
                    val error = Gson().fromJson(it.string(), ErrorResponse::class.java)
                    throw NetworkException(error.errorDescription)
                } ?: throw NetworkException("Unable to introspect token")
            }
        }
    }

    internal class NetworkException(message: String?, cause: Exception? = null) :
        Exception(message, cause)
}
