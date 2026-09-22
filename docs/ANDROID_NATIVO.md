# CHETO Authenticator — Android nativo

CHETO 1.0.0 usa Kotlin y Jetpack Compose. TOTP funciona sin servidor.

## Requisitos

- Android 8.0 / API 26 o superior.
- Hora automática recomendada.
- Biometría opcional.
- Internet solo para funciones cloud o identidad; TOTP funciona offline.

## Componentes

- NativeActivity.kt: Android, biometría, archivos, QR y OAuth.
- ui/: interfaz Compose.
- data/NativeVault.kt: bóveda y .cheto.
- security/: protección local.
- core/TotpEngine.kt: TOTP.
- backup/: cifrado, WorkManager, Drive y OneDrive.
- identity/: Google y Microsoft.

## Backup local

El formato actual serializa, comprime con GZIP y cifra con AES-GCM. La restauración conserva compatibilidad con la versión anterior soportada.

No confundir PIN local con contraseña del backup.

## Backup cloud

Google:

    Mi unidad/CHETO Authenticator/Backups/cheto_native_backup_current.cheto

OneDrive:

    Apps/CHETO Authenticator/cheto_native_backup_current.cheto

Solo se mantiene la copia actual. Antes de tocar cloud se compara SHA-256 local. Sin cambios no se autoriza ni consume API.

WorkManager agenda ciclos aproximados de 24 horas con red cuando el proveedor está habilitado.

## Build

Debug:

    .\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug

Release:

    .\gradlew.bat clean testDebugUnitTest lintDebug assembleDebugAndroidTest assembleRelease bundleRelease

Para release ver RELEASE.md.
