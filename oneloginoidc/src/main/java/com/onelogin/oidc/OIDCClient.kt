package com.onelogin.oidc

import android.app.Activity
import com.onelogin.oidc.introspect.IntrospectionError
import com.onelogin.oidc.introspect.TokenIntrospection
import com.onelogin.oidc.login.SignInError
import com.onelogin.oidc.login.SignInSuccess
import com.onelogin.oidc.refresh.RefreshError
import com.onelogin.oidc.refresh.RefreshSuccess
import com.onelogin.oidc.revoke.RevokeError
import com.onelogin.oidc.revoke.RevokeSuccess
import com.onelogin.oidc.session.SessionInfo
import com.onelogin.oidc.session.SessionInfoError
import com.onelogin.oidc.userInfo.UserInfo
import com.onelogin.oidc.userInfo.UserInfoError

interface OIDCClient {

    /**
     * Call this method in order to execute the sign in process, which will open a custom tab or a browser
     *
     * @param activity Activity that will own the sign in process, process will be attached to its lifecycle
     * @param signInCallback Callback to receive the status of the signIn process, in case of success the
     * {@link com.onelogin.oidc.session.SessionInfo} will be returned with the appropriate status of the session and the tokens
     */
    fun signIn(activity: Activity, signInCallback: Callback<SignInSuccess, SignInError>)

    /**
     * Call this method in order to invalidate a previous session and all the tokens related to this,
     * when we logout the user we should call this method in order to ensure that the tokens can still be
     * used without user knowledge
     *
     * @param revokeTokenCallback Callback to receive the result of the revoke token operation it will
     * return an string describing the result if successful
     */
    fun revokeToken(revokeTokenCallback: Callback<RevokeSuccess, RevokeError>)

    /**
     * Triggers a manual refresh of the token in case that we want to run this process manually
     *
     * @param refreshTokenCallback Callback to receive the refresh token operation result, it will
     * return the {@link com.onelogin.oidc.session.SessionInfo} with the updated tokens in case of success
     */
    fun refreshToken(refreshTokenCallback: Callback<RefreshSuccess, RefreshError>)

    /**
     * Gets the stored info of the user
     *
     * @param getUserInfoCallback Callback to listen for the information, it will return the stored
     * user info in case of success
     */
    fun getUserInfo(getUserInfoCallback: Callback<UserInfo, UserInfoError>)

    /**
     * Introspects the current token and give us metadata related to it
     *
     * @param introspectionCallback Callback to listen for the result of the operation, will return the
     * token metadata in case of success
     */
    fun introspect(introspectionCallback: Callback<TokenIntrospection, IntrospectionError>)

    /**
     * Gets the latest stored session info including tokens and expiration time, use it to make requests
     * or know if the token needs to be refreshed
     *
     * @return Returns the information of the latest stored session
     */
    @Throws(SessionInfoError::class)
    fun getSessionInfo(): SessionInfo

    /**
     * Cancels all the pending operations related to this client, use this to avoid leaks of the callbacks
     * and to free resources in case that the result of an operation is no longer required
     */
    fun cancel()
}
