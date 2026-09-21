package com.cmcorpusg.chetoauthenticator.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectiveExportPolicyTest {
    @Test
    fun selectedExportContainsOnlyRequestedAccountsAndNoProfileIdentityOrTrash() {
        val source = MobileVault(
            pin = "123456",
            name = "Persona",
            emails = listOf("persona@example.com"),
            photo = "avatar",
            categories = listOf("Sin categoría", "Trabajo", "Social"),
            accounts = listOf(
                MobileAccount(id = "a", issuer = "GitHub", label = "a@example.com", secret = "AAAA", category = "Trabajo"),
                MobileAccount(id = "b", issuer = "Discord", label = "b@example.com", secret = "BBBB", category = "Social")
            ),
            categoryColors = mapOf("Trabajo" to "#111111", "Social" to "#222222"),
            linkedIdentities = listOf(
                LinkedIdentity("google", "subject", "persona@example.com")
            ),
            trash = listOf(
                TrashedAccount(MobileAccount(id = "c", issuer = "Steam", label = "c", secret = "CCCC"))
            )
        )

        val exported = SelectiveExportPolicy.create(source, setOf("a"))

        assertEquals(listOf("a"), exported.accounts.map { it.id })
        assertEquals("", exported.name)
        assertTrue(exported.emails.isEmpty())
        assertEquals("", exported.photo)
        assertTrue(exported.linkedIdentities.isEmpty())
        assertTrue(exported.trash.isEmpty())
        assertTrue("Trabajo" in exported.categories)
        assertTrue("Social" !in exported.categories)
        assertEquals(mapOf("Trabajo" to "#111111"), exported.categoryColors)
    }
}
