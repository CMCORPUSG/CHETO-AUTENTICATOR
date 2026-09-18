# CHETO-AUTENTICATOR

Autenticador TOTP/2FA para Android, orientado a uso personal y con código fuente auditable.

## V1

- QR estándar `otpauth://totp`.
- Registro manual mediante clave TOTP.
- Compatible con servicios que usan TOTP estándar, como Google, Microsoft, Twitch, Kick y otros.
- Códigos de 6 u 8 dígitos.
- SHA-1, SHA-256 y SHA-512.
- Generación offline según RFC 6238.
- Cuentas cifradas localmente con AES-GCM y Android Keystore.
- Bloqueo biométrico cuando el dispositivo dispone de biometría fuerte.
- Copia cifrada en Google Drive `appDataFolder`.
- Backup periódico cada 24 horas mediante WorkManager.
- Backup inmediato después de cambios y botón manual.
- Retención de los 7 backups más recientes.
- Restauración en otro Android mediante Google Drive + contraseña de recuperación.

## Privacidad

GitHub contiene **solo el código fuente**.

No deben subirse:

- secretos TOTP;
- QR reales;
- códigos 2FA;
- contraseñas de recuperación;
- backups `.enc`;
- tokens OAuth;
- archivos `google-services.json`;
- keystores o credenciales de firma.

La lista completa está protegida por `.gitignore` y documentada en [SECURITY.md](SECURITY.md).

## Google Drive

La app solicita únicamente el scope `drive.appdata`, no acceso general al Drive. La configuración de OAuth Android y SHA-1 está explicada en [docs/GOOGLE_DRIVE_SETUP.md](docs/GOOGLE_DRIVE_SETUP.md).

## Build

Requisitos del proyecto:

- JDK 17
- Android SDK
- compileSdk 36 / targetSdk 36
- Gradle 9.6
- Android Gradle Plugin 9.4.0

Desde Android Studio se puede abrir la raíz del repositorio y sincronizar Gradle.

GitHub Actions ejecuta automáticamente pruebas unitarias y genera un APK debug como artefacto del workflow.

## Seguridad del backup

La contraseña de recuperación no se guarda. Se deriva una clave con PBKDF2-HMAC-SHA256; la clave derivada que necesita el backup automático se envuelve con una clave de Android Keystore. Los archivos remotos se cifran con AES-256-GCM antes de salir del dispositivo.
