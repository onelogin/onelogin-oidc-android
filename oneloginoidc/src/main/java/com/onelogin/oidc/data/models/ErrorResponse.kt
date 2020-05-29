package com.onelogin.oidc.data.models

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("error")
    val error: String,
    @SerializedName("error_description")
    val errorDescription: String
)
