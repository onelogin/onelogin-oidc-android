package com.onelogin.oidc.login

import com.onelogin.oidc.session.SessionInfo

data class SignInSuccess(
    val sessionInfo: SessionInfo
)
