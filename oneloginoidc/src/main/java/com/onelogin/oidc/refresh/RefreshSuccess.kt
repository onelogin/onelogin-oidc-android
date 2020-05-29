package com.onelogin.oidc.refresh

import com.onelogin.oidc.session.SessionInfo

data class RefreshSuccess(
    val sessionInfo: SessionInfo
)
