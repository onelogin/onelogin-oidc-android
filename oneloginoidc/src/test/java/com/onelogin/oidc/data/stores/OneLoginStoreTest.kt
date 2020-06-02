package com.onelogin.oidc.data.stores

import android.content.SharedPreferences
import com.onelogin.oidc.TestRail
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OneLoginStoreTest {

    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()

    private lateinit var oneLoginStore: OneLoginStore

    @Before
    fun setup() {
        oneLoginStore = OneLoginStore(preferences)
        every { preferences.edit() }.returns(editor)
        every { preferences.getString(any(), any()) }.returns("testValue")
        every { editor.putString(any(), any()) }.returns(editor)
        every { editor.remove(any()) }.returns(editor)
        every { editor.commit() }.returns(true)
    }

    @Test
    @TestRail
    fun testPersist() {
        oneLoginStore.persist("testKey", "testData")

        verify { preferences.edit() }
        verify { editor.putString("testKey", "testData") }
        verify { editor.commit() }
    }

    @Test
    @TestRail
    fun testFetch() {
        val result = oneLoginStore.fetch("testKey")

        verify { preferences.getString("testKey", null) }
        assertEquals("testValue", result)
    }

    @Test
    @TestRail
    fun testClear() {
        oneLoginStore.clear("testKey")

        verify { preferences.edit() }
        verify { editor.remove("testKey") }
        verify { editor.commit() }
    }
}
