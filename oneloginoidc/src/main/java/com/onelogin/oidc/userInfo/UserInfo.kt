import com.google.gson.annotations.SerializedName

data class UserInfo(
    @SerializedName("sub")
    val sub: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("preferred_username")
    val preferredUsername: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("updated_at")
    val updatedAt: Long?,
    @SerializedName("given_name")
    val givenName: String?,
    @SerializedName("family_name")
    val familyName: String?,
    @SerializedName("groups")
    val groups: List<String>?
)
