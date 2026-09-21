package com.cmcorpusg.chetoauthenticator

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.cmcorpusg.chetoauthenticator.backup.BackupScheduler
import com.cmcorpusg.chetoauthenticator.backup.BackupSettings

class ChetoAuthenticatorApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        val backup = BackupSettings(this)
        if (backup.driveEnabled || backup.oneDriveEnabled) {
            BackupScheduler.schedule(this)
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("cheto_logo_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
}
