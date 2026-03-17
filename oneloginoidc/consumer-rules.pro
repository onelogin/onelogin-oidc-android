# Keep Gson model classes used for OIDC response deserialization
-keepclassmembers class com.onelogin.oidc.userInfo.UserInfo { *; }
-keepclassmembers class com.onelogin.oidc.introspect.TokenIntrospection { *; }
-keepclassmembers class com.onelogin.oidc.data.network.ErrorResponse { *; }
-keepattributes Signature
-keepattributes *Annotation*
