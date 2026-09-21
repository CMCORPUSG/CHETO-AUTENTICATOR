package com.cmcorpusg.chetoauthenticator.backup

import android.content.Context

class BackupSettings(context: Context) {
    private val prefs = context.getSharedPreferences("cheto_backup_settings", Context.MODE_PRIVATE)

    var driveEnabled: Boolean
        get() = prefs.getBoolean(KEY_DRIVE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DRIVE_ENABLED, value).apply()

    var oneDriveEnabled: Boolean
        get() = prefs.getBoolean(KEY_ONEDRIVE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONEDRIVE_ENABLED, value).apply()

    var lastOneDriveBackupEpochMillis: Long
        get() = prefs.getLong(KEY_LAST_ONEDRIVE_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ONEDRIVE_BACKUP, value).apply()

    var lastBackupEpochMillis: Long
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP, value).apply()

    var lastError: String?
        get() = prefs.getString(KEY_LAST_ERROR, null)
        set(value) = prefs.edit().putString(KEY_LAST_ERROR, value).apply()

    var lastVerifiedBackupEpochMillis: Long
        get() = prefs.getLong(KEY_LAST_VERIFIED_BACKUP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_VERIFIED_BACKUP, value).apply()

    companion object {
        private const val KEY_DRIVE_ENABLED = "drive_enabled"
        private const val KEY_ONEDRIVE_ENABLED = "onedrive_enabled"
        private const val KEY_LAST_ONEDRIVE_BACKUP = "last_onedrive_backup"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_LAST_VERIFIED_BACKUP = "last_verified_backup"
    }
}
