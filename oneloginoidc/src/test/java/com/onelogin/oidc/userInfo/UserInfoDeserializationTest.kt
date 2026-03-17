package com.onelogin.oidc.userInfo

import com.google.gson.Gson
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class UserInfoDeserializationTest {

    private val gson = Gson()

    @Test
    fun deserializesSnakeCaseFieldsCorrectly() {
        val json = """
            {
              "sub": "12345",
              "email": "user@example.com",
              "preferred_username": "jdoe",
              "name": "Jane Doe",
              "updated_at": 1710000000,
              "given_name": "Jane",
              "family_name": "Doe",
              "groups": ["admins", "developers"]
            }
        """.trimIndent()

        val result = gson.fromJson(json, UserInfo::class.java)

        assertEquals("12345", result.sub)
        assertEquals("user@example.com", result.email)
        assertEquals("jdoe", result.preferredUsername)
        assertEquals("Jane Doe", result.name)
        assertEquals(1710000000L, result.updatedAt)
        assertEquals("Jane", result.givenName)
        assertEquals("Doe", result.familyName)
        assertEquals(listOf("admins", "developers"), result.groups)
    }

    @Test
    fun handlesNullOptionalFields() {
        val json = """
            {
              "sub": "12345",
              "email": "user@example.com"
            }
        """.trimIndent()

        val result = gson.fromJson(json, UserInfo::class.java)

        assertEquals("12345", result.sub)
        assertEquals("user@example.com", result.email)
        assertNull(result.preferredUsername)
        assertNull(result.name)
        assertNull(result.updatedAt)
        assertNull(result.givenName)
        assertNull(result.familyName)
        assertNull(result.groups)
    }
}
