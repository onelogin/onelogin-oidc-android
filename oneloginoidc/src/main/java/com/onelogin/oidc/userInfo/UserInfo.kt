package com.onelogin.oidc.userInfo

data class UserInfo(
    val sub: String,
    val email: String,
    val preferredUsername: String?,
    val name: String?,
    val updatedAt: String?,
    val givenName: String?,
    val familyName: String?,
    val groups: List<String>?
)
