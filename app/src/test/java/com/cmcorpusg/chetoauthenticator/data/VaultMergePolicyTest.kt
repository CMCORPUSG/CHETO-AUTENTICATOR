package com.cmcorpusg.chetoauthenticator.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultMergePolicyTest {
    @Test
    fun mergeKeepsCurrentSecurityAndAddsOnlyMissingRecoveryData() {
        val current = MobileVault(
            pin = "654321",
            name = "Actual",
            emails = listOf("actual@example.com"),
            categories = listOf("Trabajo"),
            accounts = listOf(
                MobileAccount(
                    id = "a1",
                    issuer = "GitHub",
                    label = "actual@example.com",
                    secret = "AAAA",
                    category = "Trabajo"
                )
            ),
            dark = true,
            hideCodes = true,
            biometric = true,
            lockTimeoutSeconds = 60,
            categoryColors = mapOf("Trabajo" to "#111111")
        )
        val restored = MobileVault(
            pin = "111111",
            name = "Backup",
            emails = listOf("actual@example.com", "backup@example.com"),
            categories = listOf("Trabajo", "Social"),
            accounts = listOf(
                MobileAccount(
                    id = "b1",
                    issuer = "GitHub",
                    label = "actual@example.com",
                    secret = "AAAA",
                    category = "Trabajo"
                ),
                MobileAccount(
                    id = "b2",
                    issuer = "Google",
                    label = "backup@example.com",
                    secret = "BBBB",
                    category = "Social"
                )
            ),
            categoryColors = mapOf("Trabajo" to "#999999", "Social" to "#222222"),
            linkedIdentities = listOf(
                LinkedIdentity(
                    provider = "google",
                    subject = "sub-1",
                    email = "backup@example.com"
                )
            ),
            trash = listOf(
                TrashedAccount(
                    account = MobileAccount(
                        id = "trash-google",
                        issuer = "Steam",
                        label = "old@example.com",
                        secret = "CCCC"
                    ),
                    deletedAtEpochMillis = 42L
                ),
                TrashedAccount(
                    account = MobileAccount(
                        id = "trash-duplicate",
                        issuer = "GitHub",
                        label = "actual@example.com",
                        secret = "AAAA"
                    ),
                    deletedAtEpochMillis = 43L
                )
            )
        )

        val merged = VaultMergePolicy.merge(current, restored)

        assertEquals("654321", merged.pin)
        assertEquals(true, merged.dark)
        assertEquals(true, merged.hideCodes)
        assertEquals(true, merged.biometric)
        assertEquals(60, merged.lockTimeoutSeconds)

        assertEquals(2, merged.accounts.size)
        assertTrue(merged.accounts.any { it.issuer == "Google" })
        assertEquals(listOf("actual@example.com", "backup@example.com"), merged.emails)
        assertTrue("Social" in merged.categories)
        assertEquals("#111111", merged.categoryColors["Trabajo"])
        assertEquals("#222222", merged.categoryColors["Social"])
        assertEquals(1, merged.linkedIdentities.size)
        assertEquals(1, merged.trash.size)
        assertEquals("Steam", merged.trash.single().account.issuer)
    }
}
