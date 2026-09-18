package com.cmcorpusg.chetoauthenticator

import android.app.Application
import com.cmcorpusg.chetoauthenticator.backup.BackupScheduler
import com.cmcorpusg.chetoauthenticator.backup.BackupSettings

class ChetoAuthenticatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BackupSettings(this).driveEnabled) {
            BackupScheduler.schedule(this)
        }
    }
}
