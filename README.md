<div align="center">

<img src="docs/assets/logo.png" width="140" alt="CHETO Authenticator">

# CHETO Authenticator

**Secure TOTP / 2FA for Android**

**Offline-first | Encrypted Vault | Google Drive | OneDrive | PIN | Biometrics**

[DESCARGAR APK V1.0.0](https://github.com/CMCORPUSG/CHETO-AUTENTICATOR/releases/download/v1.0.0/CHETO-Authenticator-1.0.0.apk)

[Release](https://github.com/CMCORPUSG/CHETO-AUTENTICATOR/releases/tag/v1.0.0) | [Instalacion](docs/INSTALLATION.md) | [Seguridad](SECURITY.md) | [Privacidad](docs/PRIVACY.md)

</div>

---

# CHETO Authenticator

CHETO Authenticator es una aplicación Android nativa para administrar códigos TOTP/2FA de forma local, con bóveda cifrada, PIN, biometría, importación por QR y respaldos cifrados locales y opcionales en Google Drive y Microsoft OneDrive.

Versión estable: 1.0.0  
Package Android: com.cmcorpusg.chetoauthenticator  
Android mínimo: Android 8.0 / API 26  
Target: API 36  
Tecnología: Kotlin + Jetpack Compose

Este repositorio contiene código fuente. No debe contener contraseñas, secretos TOTP, QR reales, tokens OAuth, backups personales ni claves privadas de firma.

## Funciones principales

- TOTP offline compatible con RFC 6238.
- Códigos de 6 u 8 dígitos con SHA-1, SHA-256 y SHA-512.
- Alta manual, QR otpauth y migración de Google Authenticator.
- Categorías, favoritos, búsqueda, edición, selección múltiple y papelera.
- Perfil local con nombre, username, correos e imagen.
- PIN de seis dígitos, biometría, bloqueo automático y protección de pantalla.
- Bóveda local cifrada con Android Keystore.
- Exportación, verificación y restauración cifrada .cheto.
- Compresión GZIP sin pérdida antes del cifrado del backup.
- Google Drive y Microsoft OneDrive opcionales.
- Backup periódico aproximado cada 24 horas cuando el proveedor está habilitado y hay red.
- SHA-256 local antes de contactar al proveedor cloud.
- Si no cambió la bóveda, el ciclo automático no autentica ni sube nada.
- Una única copia cloud actual por proveedor.
- Estado, correo conectado, permisos, ruta y botón para abrir la ubicación cloud.

## Si alguien solo quiere usar la app

La forma recomendada es instalar un APK release oficial publicado en GitHub Releases.

1. Descargar el APK de la versión estable.
2. Instalarlo en Android.
3. Crear un perfil local y PIN, o restaurar un archivo .cheto.
4. Guardar la contraseña del backup en un lugar seguro. CHETO no puede recuperarla.

Si una actualización usa el mismo certificado de firma y un versionCode mayor, Android permite instalarla encima. Si cambia la firma, primero se debe exportar y verificar un .cheto, guardar una copia fuera del teléfono y recién después reinstalar.

Guía completa: docs/INSTALLATION.md

## Si alguien quiere compilar el proyecto

Requisitos:

- JDK 17.
- Android SDK API 36.
- Git.
- Android Studio compatible o terminal.
- Gradle Wrapper incluido.

Clonar:

    git clone https://github.com/CMCORPUSG/CHETO-AUTENTICATOR.git
    cd CHETO-AUTENTICATOR

En Windows, crear local.properties con la ruta local del SDK. Ejemplo:

    sdk.dir=C:\Users\TU_USUARIO\AppData\Local\Android\Sdk

Compilar debug:

    .\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug

APK debug:

    app/build/outputs/apk/debug/app-debug.apk

Para release se necesita un keystore propio y keystore.properties local. Ver docs/RELEASE.md.

## Cloud en forks y builds propios

TOTP y backup local funcionan sin Google ni Microsoft.

Un APK compilado por otra persona tendrá una firma distinta. Para usar Drive o OneDrive debe registrar su propia firma Android en los proveedores OAuth.

Documentación:

- docs/GOOGLE_DRIVE_SETUP.md
- docs/IDENTITY_SETUP.md
- docs/RELEASE.md

Una app Android es un cliente público: no se debe incrustar un client_secret.

## Rutas de backup

Google Drive:

    Mi unidad
    └── CHETO Authenticator
        └── Backups
            └── cheto_native_backup_current.cheto

OneDrive:

    Apps
    └── CHETO Authenticator
        └── cheto_native_backup_current.cheto

## Cómo funciona el backup automático

Cuando un proveedor está habilitado, WorkManager agenda una ejecución aproximadamente cada 24 horas y requiere conexión de red.

Antes de tocar cloud, CHETO:

1. genera la representación portable local;
2. calcula SHA-256;
3. compara con el último backup exitoso;
4. si es igual, termina sin autenticar ni usar API;
5. si cambió, comprime con GZIP;
6. cifra con AES-256-GCM;
7. reemplaza cheto_native_backup_current.cheto.

## Seguridad

Nunca subir al repositorio:

- archivos .cheto;
- .jks o .keystore;
- keystore.properties;
- google-services.json;
- client_secret;
- tokens OAuth;
- contraseñas;
- QR o TOTP reales.

Ver SECURITY.md y docs/PRIVACY.md.

## Estructura técnica

- NativeActivity.kt: ciclo de vida, biometría, archivos, QR y OAuth.
- ui/: pantallas Compose.
- data/NativeVault.kt: bóveda y .cheto.
- core/TotpEngine.kt: TOTP.
- backup/: cifrado, WorkManager, Google Drive y OneDrive.
- identity/: identidad Google/Microsoft.

Notas de versión: docs/releases/V1.0.0.md
