package com.onelogin.oidc.logout

import android.app.Activity
import com.onelogin.oidc.Callback

interface SignOutManager {
    suspend fun singOut(
        idToken: String,
        activity: Activity,
        signOutCallback: Callback<SignOutSuccess, SignOutError>
    )
}
