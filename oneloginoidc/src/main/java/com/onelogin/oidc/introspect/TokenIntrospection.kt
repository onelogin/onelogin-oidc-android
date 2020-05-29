package com.onelogin.oidc.introspect

import com.google.gson.annotations.SerializedName

data class TokenIntrospection(
    @SerializedName("active")
    val active: Boolean,
    @SerializedName("token_type")
    val tokenType: String?,
    @SerializedName("sub")
    val sub: String?,
    @SerializedName("client_id")
    val clientId: String?,
    @SerializedName("exp")
    val exp: String?,
    @SerializedName("iat")
    val iat: String?,
    @SerializedName("iss")
    val iss: String?,
    @SerializedName("jti")
    val jti: String?
)
