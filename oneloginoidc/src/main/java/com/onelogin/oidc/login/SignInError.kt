package com.onelogin.oidc.login

class SignInError(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
