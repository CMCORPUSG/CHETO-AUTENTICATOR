# Guía de release — CHETO Authenticator

## 1. Firma

El keystore release permite futuras actualizaciones. Nunca subirlo a Git.

## 2. Crear keystore

    New-Item -ItemType Directory ".\signing" -Force
    & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -keystore ".\signing\cheto-release.jks" -storetype JKS -alias "cheto-release" -keyalg RSA -keysize 4096 -validity 10000

Usar una contraseña fuerte y no publicarla.

## 3. keystore.properties

    Copy-Item ".\keystore.properties.example" ".\keystore.properties"

Contenido local:

    storeFile=signing/cheto-release.jks
    storePassword=CONTRASEÑA_LOCAL
    keyAlias=cheto-release
    keyPassword=CONTRASEÑA_LOCAL

## 4. Verificar certificado

    & "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore ".\signing\cheto-release.jks" -alias "cheto-release"

## 5. OAuth release

Google: package + SHA-1 release y scopes drive.file + drive.appdata.

Microsoft: plataforma Android con package + hash de firma y permisos Files.ReadWrite.AppFolder + User.Read.

## 6. Versionado V1.0.0

    versionCode = 18
    versionName = "1.0.0"

## 7. Build completo

    .\gradlew.bat clean testDebugUnitTest lintDebug assembleDebugAndroidTest assembleRelease bundleRelease

Artefactos:

    app/build/outputs/apk/release/app-release.apk
    app/build/outputs/bundle/release/app-release.aab

## 8. Verificar firma

    $apksigner = Get-ChildItem "$env:ANDROID_HOME\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1 | ForEach-Object { Join-Path $_.FullName "apksigner.bat" }
    & $apksigner verify --print-certs ".\app\build\outputs\apk\release\app-release.apk"

## 9. Hashes

    Get-FileHash ".\app\build\outputs\apk\release\app-release.apk" -Algorithm SHA256
    Get-FileHash ".\app\build\outputs\bundle\release\app-release.aab" -Algorithm SHA256

## 10. Prueba física

Validar arranque minificado, PIN, biometría, QR, TOTP, perfil, .cheto, restauración limpia, Google Drive, OneDrive y actualización con la misma firma.

## 11. Publicar en GitHub Releases

No commitear APK o AAB en el árbol fuente.

1. Crear tag v1.0.0.
2. Crear Release desde el tag.
3. Adjuntar app-release.apk.
4. Opcionalmente adjuntar app-release.aab.
5. Publicar SHA-256.
6. Enlazar docs/releases/V1.0.0.md.

El AAB no se instala directamente en un teléfono.

## 12. Respaldo de firma

Mantener al menos dos copias seguras y offline del keystore.

## 13. Revisión antes de push

    git status --short
    git check-ignore -v ".\keystore.properties"
    git check-ignore -v ".\signing\cheto-release.jks"
