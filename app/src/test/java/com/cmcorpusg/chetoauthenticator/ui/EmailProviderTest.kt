package com.cmcorpusg.chetoauthenticator.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailProviderTest {
    @Test
    fun detectsCommonProviders() {
        assertEquals("Gmail", EmailProvider.nameFor("demo@gmail.com"))
        assertEquals("Outlook", EmailProvider.nameFor("demo@outlook.com"))
        assertEquals("Hotmail", EmailProvider.nameFor("demo@hotmail.com"))
        assertEquals("Apple", EmailProvider.nameFor("demo@icloud.com"))
    }
}
