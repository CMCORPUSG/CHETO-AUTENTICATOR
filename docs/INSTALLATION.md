# Instalación y recuperación — CHETO Authenticator

## Instalar un APK release

1. Descargar el APK desde GitHub Releases.
2. Abrirlo desde el gestor de archivos.
3. Autorizar temporalmente la instalación desde esa fuente si Android lo solicita.
4. Abrir CHETO.

No se necesita Android Studio para usar un APK ya compilado.

### Instalar con ADB

    adb devices
    adb -s SERIAL install "ruta\al\app-release.apk"

Si Xiaomi o Android responde INSTALL_FAILED_USER_RESTRICTED, copiar el APK e instalarlo manualmente:

    adb -s SERIAL push ".\app\build\outputs\apk\release\app-release.apk" "/sdcard/Download/CHETO-release.apk"

Luego abrir Download/CHETO-release.apk en el teléfono.

## Primera ejecución

Se puede crear un perfil local o recuperar una bóveda desde .cheto.

- PIN: protege el acceso local.
- Contraseña del backup: descifra la copia portable o cloud.

Son credenciales diferentes.

## Antes de desinstalar o cambiar de firma

1. Exportar un .cheto.
2. Usar Verificar copia dentro de CHETO.
3. Guardar una segunda copia fuera del teléfono.
4. Opcionalmente calcular SHA-256.
5. Recién después desinstalar.

Ejemplo:

    Get-FileHash "D:\BACKUP\archivo.cheto" -Algorithm SHA256

Android no permite actualizar el mismo package con un certificado de firma diferente.

## Restaurar

1. Instalar y abrir CHETO.
2. Elegir recuperación o restauración.
3. Seleccionar el archivo .cheto.
4. Introducir la contraseña del backup.
5. Definir PIN si el flujo lo solicita.
6. Revisar cuentas, categorías y perfil.

La contraseña del backup no se guarda dentro del archivo.

## Compilar desde código

Requisitos: Git, JDK 17, Android SDK API 36 y Android build-tools.

    git clone https://github.com/CMCORPUSG/CHETO-AUTENTICATOR.git
    cd CHETO-AUTENTICATOR

Windows:

    .\gradlew.bat --version
    .\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug

macOS/Linux:

    ./gradlew --version
    ./gradlew clean testDebugUnitTest lintDebug assembleDebug

APK debug:

    app/build/outputs/apk/debug/app-debug.apk

## Cloud en una compilación propia

No es obligatorio para TOTP local.

Google Drive requiere:

- Drive API habilitada.
- Google Auth Platform configurado.
- Package com.cmcorpusg.chetoauthenticator.
- SHA-1 del certificado que firma el APK.
- Scopes drive.file y drive.appdata.

OneDrive requiere:

- Microsoft Entra App Registration.
- Plataforma Android.
- Hash de firma del APK.
- Files.ReadWrite.AppFolder.
- User.Read.

## Actualizaciones

Si applicationId y certificado coinciden y el nuevo versionCode es mayor, Android puede actualizar encima.

Antes de actualizaciones importantes se recomienda exportar y verificar .cheto.

## Nunca compartir

No publicar keystore release, contraseñas, keystore.properties, backups reales, secretos TOTP, QR reales ni tokens.
