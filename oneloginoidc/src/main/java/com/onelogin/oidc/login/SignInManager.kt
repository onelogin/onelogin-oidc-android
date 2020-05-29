package com.onelogin.oidc.login

import android.app.Activity
import com.onelogin.oidc.Callback

interface SignInManager {
    suspend fun signIn(activity: Activity, signInCallback: Callback<SignInSuccess, SignInError>)
}
