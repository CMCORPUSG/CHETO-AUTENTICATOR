package com.cmcorpusg.chetoauthenticator.backup

import com.cmcorpusg.chetoauthenticator.data.MobileAccount
import com.cmcorpusg.chetoauthenticator.data.MobileVault
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryHealthPolicyTest {
    @Test
    fun readyWhenBackupIsRecentVerifiedAndRecoveryKeyExists() {
        val now = 1_000_000_000L
        val vault = MobileVault(
            accounts = listOf(MobileAccount(issuer = "Google", label = "a", secret = "AAAA")),
            trashRetentionDays = 30
        )

        val health = RecoveryHealthPolicy.evaluate(
            vault = vault,
            driveEnabled = true,
            lastBackupEpochMillis = now - 60_000L,
            lastVerifiedBackupEpochMillis = now - 60_000L,
            recoveryKeyConfigured = true,
            nowEpochMillis = now
        )

        assertEquals(5, health.score)
        assertEquals("Lista", health.label)
    }

    @Test
    fun incompleteWhenNoBackupOrVerificationExists() {
        val vault = MobileVault(
            accounts = listOf(MobileAccount(issuer = "GitHub", label = "a", secret = "AAAA")),
            trashRetentionDays = 7
        )

        val health = RecoveryHealthPolicy.evaluate(
            vault = vault,
            driveEnabled = false,
            lastBackupEpochMillis = 0L,
            lastVerifiedBackupEpochMillis = 0L,
            recoveryKeyConfigured = false,
            nowEpochMillis = 1_000_000_000L
        )

        assertEquals("Incompleta", health.label)
    }
}
