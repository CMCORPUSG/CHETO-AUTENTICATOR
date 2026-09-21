package com.cmcorpusg.chetoauthenticator.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeVaultBackupInstrumentedTest {
    private val password = "CHETO-recovery-2026"

    @Test
    fun portableBackupRoundTripPreservesRecoveryDataAndKeepsLocalSecuritySettings() {
        val source = MobileVault(
            pin = "111111",
            name = "Usuario CHETO",
            username = "usuario.cheto",
            emails = listOf("main@example.com", "second@example.com"),
            verifiedEmails = listOf("main@example.com"),
            photo = "https://example.com/avatar.png",
            categories = listOf("Sin categoría", "Trabajo", "Social"),
            accounts = listOf(
                MobileAccount(
                    id = "account-1",
                    issuer = "GitHub",
                    label = "main@example.com",
                    secret = "JBSWY3DPEHPK3PXP",
                    category = "Trabajo",
                    notes = "Cuenta principal",
                    digits = 6,
                    period = 30,
                    algorithm = "SHA1"
                )
            ),
            categoryColors = mapOf("Trabajo" to "#3157F6"),
            linkedIdentities = listOf(
                LinkedIdentity(
                    provider = "google",
                    subject = "google-subject",
                    email = "main@example.com",
                    displayName = "Usuario CHETO",
                    photo = "https://example.com/google.png",
                    linkedAtEpochMillis = 123456789L
                )
            ),
            trash = listOf(
                TrashedAccount(
                    account = MobileAccount(
                        id = "trash-1",
                        issuer = "Discord",
                        label = "old@example.com",
                        secret = "JBSWY3DPEHPK3PXQ",
                        category = "Social"
                    ),
                    deletedAtEpochMillis = 987654321L
                )
            )
        )

        val localDevice = MobileVault(
            pin = "654321",
            dark = true,
            hideCodes = true,
            biometric = true,
            screenshots = false,
            lockTimeoutSeconds = 60,
            clipboardClearSeconds = 15,
            reauthOnReveal = true,
            trashRetentionDays = 90
        )

        val encrypted = NativeVault.export(source, password)
        val restored = NativeVault.restore(encrypted, password, localDevice)

        assertEquals(source.name, restored.name)
        assertEquals(source.username, restored.username)
        assertEquals(source.emails, restored.emails)
        assertEquals(source.verifiedEmails, restored.verifiedEmails)
        assertEquals(source.photo, restored.photo)
        assertEquals(source.categories, restored.categories)
        assertEquals(source.accounts, restored.accounts)
        assertEquals(source.categoryColors, restored.categoryColors)
        assertEquals(source.linkedIdentities, restored.linkedIdentities)
        assertEquals(source.trash, restored.trash)

        // Portable backups intentionally do not replace device-local security settings.
        assertEquals(localDevice.pin, restored.pin)
        assertEquals(localDevice.dark, restored.dark)
        assertEquals(localDevice.hideCodes, restored.hideCodes)
        assertEquals(localDevice.biometric, restored.biometric)
        assertEquals(localDevice.screenshots, restored.screenshots)
        assertEquals(localDevice.lockTimeoutSeconds, restored.lockTimeoutSeconds)
        assertEquals(localDevice.clipboardClearSeconds, restored.clipboardClearSeconds)
        assertEquals(localDevice.reauthOnReveal, restored.reauthOnReveal)
        assertEquals(localDevice.trashRetentionDays, restored.trashRetentionDays)

        val inspection = NativeVault.inspectBackup(encrypted, password)
        assertEquals(source.accounts.size, inspection.accountCount)
        assertEquals(source.categories.size, inspection.categoryCount)
        assertEquals(source.linkedIdentities.size, inspection.linkedIdentityCount)
        assertEquals(source.trash.size, inspection.trashedAccountCount)
    }

    @Test
    fun wrongRecoveryPasswordCannotDecryptBackup() {
        val source = MobileVault(
            pin = "111111",
            accounts = listOf(
                MobileAccount(
                    issuer = "Google",
                    label = "main@example.com",
                    secret = "JBSWY3DPEHPK3PXP"
                )
            )
        )
        val encrypted = NativeVault.export(source, password)

        assertThrows(Exception::class.java) {
            NativeVault.restore(encrypted, "incorrect-password", MobileVault(pin = "654321"))
        }
    }
}
