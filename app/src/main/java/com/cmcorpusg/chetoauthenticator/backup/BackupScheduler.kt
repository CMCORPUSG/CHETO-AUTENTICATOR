package com.cmcorpusg.chetoauthenticator.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val GOOGLE_PERIODIC_NAME = "cheto-google-drive-backup-24h"
    private const val ONEDRIVE_PERIODIC_NAME = "cheto-onedrive-backup-24h"

    fun schedule(context: Context) {
        val settings = BackupSettings(context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val manager = WorkManager.getInstance(context)

        if (settings.driveEnabled) {
            val google = PeriodicWorkRequestBuilder<NativeDriveBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            manager.enqueueUniquePeriodicWork(
                GOOGLE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                google
            )
        } else {
            manager.cancelUniqueWork(GOOGLE_PERIODIC_NAME)
        }

        if (settings.oneDriveEnabled) {
            val microsoft = PeriodicWorkRequestBuilder<NativeOneDriveBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            manager.enqueueUniquePeriodicWork(
                ONEDRIVE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                microsoft
            )
        } else {
            manager.cancelUniqueWork(ONEDRIVE_PERIODIC_NAME)
        }
    }

    fun disableGoogle(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(GOOGLE_PERIODIC_NAME)
    }

    fun disableOneDrive(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ONEDRIVE_PERIODIC_NAME)
    }

    fun disable(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(GOOGLE_PERIODIC_NAME)
        manager.cancelUniqueWork(ONEDRIVE_PERIODIC_NAME)
    }
}
