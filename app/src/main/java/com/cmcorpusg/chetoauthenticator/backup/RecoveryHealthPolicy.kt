package com.cmcorpusg.chetoauthenticator.backup

import com.cmcorpusg.chetoauthenticator.data.MobileVault

data class RecoveryHealth(
    val score: Int,
    val total: Int,
    val label: String
)

object RecoveryHealthPolicy {
    fun evaluate(
        vault: MobileVault,
        driveEnabled: Boolean,
        lastBackupEpochMillis: Long,
        lastVerifiedBackupEpochMillis: Long,
        recoveryKeyConfigured: Boolean,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): RecoveryHealth {
        val recentBackup = lastBackupEpochMillis > 0L &&
            nowEpochMillis - lastBackupEpochMillis <= 72L * 60L * 60L * 1000L
        val recentlyVerified = lastVerifiedBackupEpochMillis > 0L &&
            nowEpochMillis - lastVerifiedBackupEpochMillis <= 30L * 24L * 60L * 60L * 1000L

        val checks = listOf(
            vault.accounts.isEmpty() || driveEnabled || lastBackupEpochMillis > 0L,
            !driveEnabled || recoveryKeyConfigured,
            recentBackup || !driveEnabled,
            recentlyVerified,
            vault.trashRetentionDays >= 30 || vault.trashRetentionDays == 0
        )
        val score = checks.count { it }
        val label = when {
            score == checks.size -> "Lista"
            score >= checks.size - 1 -> "Buena"
            score >= 3 -> "Revisar"
            else -> "Incompleta"
        }
        return RecoveryHealth(score, checks.size, label)
    }
}
