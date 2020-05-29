package com.onelogin.oidc.data.stores

import android.content.SharedPreferences

class OneLoginStore(
    private val preferences: SharedPreferences
) : Store {


    override fun persist(key: String, data: String): Boolean = with(preferences.edit()) {
        putString(key, data)
        commit()
    }

    override fun fetch(key: String): String? = preferences.getString(key, null)

    override fun clear(key: String) {
        with(preferences.edit()) {
            remove(key)
            commit()
        }
    }
}
