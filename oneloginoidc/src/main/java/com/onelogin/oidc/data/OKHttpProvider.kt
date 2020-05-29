package com.onelogin.oidc.data

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient

object OKHttpProvider {
    internal fun getOkHttpClient(context: Context) = OkHttpClient.Builder()
            .cache(Cache(context.cacheDir, 10 * 1024 * 1024))
            .build()
}
