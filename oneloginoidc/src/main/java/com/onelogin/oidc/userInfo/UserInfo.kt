package com.onelogin.oidc.userInfo

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class UserInfo(
    val sub: String,
    val email: String,
    @SerializedName("preferred_username")  val preferredUsername: String?,
    val name: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("given_name") val givenName: String?,
    @SerializedName("family_name") val familyName: String?,
    val groups: List<String>?
) : JSONConvertable

interface JSONConvertable {
    fun toJSON(): String = Gson().toJson(this)
}

inline fun <reified T: JSONConvertable> String.toObject(): T = Gson().fromJson(this, T::class.java)
