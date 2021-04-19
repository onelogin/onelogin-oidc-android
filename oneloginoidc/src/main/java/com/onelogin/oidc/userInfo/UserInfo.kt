package com.onelogin.oidc.userInfo

data class UserInfo(
    val sub: String,
    val email: String,
    @SerializedName("preferred_username")
    val preferredUsername: String?,
    val name: String?,
    @SerializedName("updated_at")
    val updatedAt: Int?,
    @SerializedName("given_name")
    val givenName: String?,
    @SerializedName("family_name")
    val familyName: String?,
    val groups: List<String>?
)
