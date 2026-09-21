package com.cmcorpusg.chetoauthenticator

import android.app.Application

class ChetoAuthenticatorApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // V3 uses NativeVault as its source of truth. The V1 periodic worker
        // still reads SecureAccountStore, so scheduling it here could upload a
        // stale legacy snapshot. Automatic V3 backups will be re-enabled only
        // after the worker is migrated to NativeVault.
    }
}
