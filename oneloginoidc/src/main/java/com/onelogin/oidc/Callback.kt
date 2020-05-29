package com.onelogin.oidc

interface Callback<Success, Error : Exception> {
    public fun onSuccess(success: Success)
    public fun onError(error: Error)
}
