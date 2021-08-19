# Android OneLogin Open ID Connect Library

This library is a kotlin wrapper for the AppAuth code to communicate with OneLogin as an OpenID Connect provider. It supports [Auth Code Flow + PKCE](https://developers.onelogin.com/openid-connect/guides/auth-flow-pkce) which is recommended for native apps. It also adds features specific to the OneLogin Provider as revoking and introspecting tokens.

To get more info about how to configure an app for OIDC visit the [Overview of OpenID Connect](https://developers.onelogin.com/openid-connect) page.

**Table of Contents**

<!-- TOC depthFrom:2 depthTo:3 -->

- [Installation](#installation)
- [Configuration](#configuration)
  - [Configure a OneLogin application](#configure-a-onelogin-application)
  - [Library Configuration](#library-configuration)
- [Authorization Redirect](#authorization-redirect)
- [API Overview](#api-overview)
  - [Callback](#callback)
  - [signIn](#signin)
  - [revokeToken](#revoketoken)
  - [refreshToken](#refreshtoken)
  - [getSessionInfo](#getsessioninfo)
  - [getUserInfo](#getuserinfo)
  - [cancel](#cancel)
- [Running Demo Apps](#running-demo-apps)

## Installation

To install the library add the following dependency to the `build.gradle` file of your module:

```
    implementation("com.onelogin:onelogin-oidc-android-sdk:1.0.0")
```


## Configuration

Before you can use OLOidc you need to configure an OneLogin application and provide some configuration parameters about your app to the framework.

### Configure a OneLogin application

Please visit the [Connect an OIDC enabled app](https://developers.onelogin.com/openid-connect/connect-to-onelogin) page for instructions on how to configure your OneLogin app.

### Library Configuration

In order to use the OIDC client the library should be initialized first, this would normally happen on the `onCreate()` method of the application.

The first step is to create an `OIDCConfiguration` instance using the builder:

```
OIDCConfiguration.Builder()
    .clientId(BuildConfig.CLIENT_ID) 
    .issuer(BuildConfig.ISSUER)
    .redirectUrl(BuildConfig.REDIRECT_URL)
    .scopes(listOf("openid"))
    .isDebug(true)
    .loginHint("test@email.com")
    .build()
```

The supported parameters of the configuration are:

| Parameter     | Description                                                                                                       | Required |
| ------------- |-------------------------------------------------------------------------------------------------------------------| -------- |
| clientId      | The Client ID of your OneLogin Application                                                                        | Required |
| issuer        | OIDC Issuer Url of your OneLogin Organization i.e. `https://example.onelogin.com/oidc/2`                          | Required |
| redirectUrl   | Redirect Url specified in the OneLogin Application                                                                | Required |
| scopes        | List of scopes of the authorization token, it should include `openid`                                             | Required |
| isDebug       | Specifies if the instance of the library should be initialized in debug mode, wich will log additional information| Optional |
| loginHint     | A string hint to the Authorization Server about the login identifier the End-User might use to log in             | Optional |

This initialization only needs to occur one time, and after this an instance of the `OIDCCLient` can be get by Calling `OneLoginOIDC.getClient()` this client is a singleton that can be used to perform any of the supported `OIDCClient` operations.

## Authorization Redirect

In order to receive the authorization redirect in the OIDC flow, the redirect scheme should be specified in `build.gradle` by adding the following lines:

```
android {
    ...
    defaultConfing {
        ...
        manifestPlaceholders = [
                "appAuthRedirectScheme": "com.example" // Replace  `com.example` with the scheme specified in your OneLogin App Configuration
        ]
        ...
    }
    ...
}
```

Once added, the library should be able to receive the authorization redirect and use the result to proceed with the flow.

## API Overview

The OIDCClient contains the methods which can be used to perform the supported OIDC operations.

### Callback

The `Callback` interface will be used to provide the results for most of the operations, this interface has two methods that should be overridden to process the results:

- `onSuccess(T: Success)` This method will be called when the operation completes successfully and will contain the relevant information of the result.

- `onError(T: Error)` This method will be called when there is an error on the operation and will return an Throwable with the information related to the error.

### signIn

The signing operation will initialize the authorization flow, by launching a browser to let the user authenticate and exchanging the authentication code for a set of tokens
that can be used to perform Authorized requests.

An `Activity` extending `AppCompatActivity`  or `FragmentActivity` is required in order to receive the result of the

Example:

```
val oidcClient = OneLoginOIDC.getClient()

oidcClient.signIn(activity, object : Callback<SignInSuccess, SignInError> {
    override fun onSuccess(success: SignInSuccess) {
       // The user has been authenticated successfully, the `success` param will contain the `SessionInfo` with the tokens ready to be used
    }

    override fun onError(loginError: SignInError) {
        // An error has occurred during the authentication process
    }
})
```

### revokeToken

Use this method to invalidate the current authorization token.

```
val oidcClient = OneLoginOIDC.getClient()

oidcClient.revokeToken(object : Callback<RevokeSuccess, RevokeError> {
    override fun onSuccess(success: RevokeSuccess) {
        // The user token has been revoked correctly
    }

    override fun onError(error: RevokeError) {
        // An error occurred during the revocation process
    }
})
```

### refreshToken

Use this method in order to trigger a token refresh manually, you can first use `getSessionInfo()` in order to verify if the token still valid.

```
val oidcClient = OneLoginOIDC.getClient()

oidcClient.refreshToken(object : Callback<RefreshSuccess, RefreshError> {

override fun onSuccess(success: RefreshSuccess) {
        // The token has been successfully refreshed, the `success` param will contain the `SessionInfo` with the tokens ready to be used
    }

    override fun onError(error: RefreshError) {
        // An error occurred while refreshing the token
    }
})
```

### getSessionInfo

Retrieves the latest SessionInfo object which contains the latest tokens and it's expiration time, the information of this object can be used to decide if  
in necessary to refresh the token, or use the contained tokens in order to authorize a request.

```
val oidcClient = OneLoginOIDC.getClient()
val sessionInfo = oidcClient.getSessionInfo()

val isAuthorized = sessionInfo.authorized // Use to verify if there is an active user session
val accessToken = sessionInfo.accessToken // Use to retrieve the latest access token to authorize a request 
val expirationTime = sessionInfo.expirationTime // Use to verify if the contained accessToken still valid 
```

### getUserInfo

Obtains the information of the current user, for more information check [Get User Info OneLogin Documentation](https://developers.onelogin.com/openid-connect/api/user-info) page.

```
val oidcClient = OneLoginOIDC.getClient()
oidcClient.getUserInfo(object : Callback<UserInfo, UserInfoError> {
    override fun onSuccess(success: UserInfo) {
        // User info has been retrieved correction success contains the current user information
    }

    override fun onError(error: UserInfoError) {
        There was an error trying to retrieve the current user information 
    }
})
```

### cancel

Cancels all pending operations and releases the callbacks, use this method in order to avoid memory leaks

```
val oidcClient = OneLoginOIDC.getClient()
oidcClient.cancel()
```

## Running Demo Apps

In order to run the demo app add the following lines to your local.properties

```
issuer="https://example.onelogin.com/oidc/2" // issuer url for your organization
client_id="Client ID" // Client ID of your OneLogin OIDC application 
redirect_url="com.example://" // Redirect url specified on your OIDC application
redirect_scheme=com.onelogin // Scheme of the redirect url specified on your OIDC application
```
