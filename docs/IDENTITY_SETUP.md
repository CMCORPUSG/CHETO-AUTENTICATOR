# Google, Microsoft y OneDrive — configuración

Las funciones cloud son opcionales. TOTP, bóveda y backup local no dependen de ellas.

## Google

Registrar com.cmcorpusg.chetoauthenticator y el SHA-1 del certificado instalado.

Propiedad local para identidad:

    CHETO_GOOGLE_WEB_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com

No incluir Google client secret.

## Microsoft Entra y OneDrive

1. Crear App registration.
2. Configurar audiencia.
3. Agregar plataforma Android.
4. Package: com.cmcorpusg.chetoauthenticator.
5. Registrar hash de firma.
6. Agregar permisos delegados:
   - Files.ReadWrite.AppFolder
   - User.Read

No incrustar client secret.

## Calcular hash MSAL

    $tmpCert = Join-Path $env:TEMP "cheto-release.cer"
    & "$env:JAVA_HOME\bin\keytool.exe" -exportcert -alias "cheto-release" -keystore ".\signing\cheto-release.jks" -file $tmpCert
    $certBytes = [System.IO.File]::ReadAllBytes($tmpCert)
    $sha1Bytes = [System.Security.Cryptography.SHA1]::Create().ComputeHash($certBytes)
    $msalHash = [Convert]::ToBase64String($sha1Bytes)
    $msalEncoded = [Uri]::EscapeDataString($msalHash)
    "MSAL_HASH=$msalHash"
    "MSAL_REDIRECT=msauth://com.cmcorpusg.chetoauthenticator/$msalEncoded"
    Remove-Item $tmpCert -Force

## Recursos MSAL

    app/src/main/res/raw/auth_config_single_account.json
    app/src/release/res/raw/auth_config_single_account.json

Un fork debe sustituir la configuración por su propia App Registration y firma.

## OneDrive

Ruta:

    OneDrive/Apps/CHETO Authenticator/cheto_native_backup_current.cheto

El worker compara SHA-256 local antes de pedir token. Sin cambios no llama a Microsoft.

## Público vs secreto

Públicos: client ID, package, fingerprints y redirect URI.

Secretos: keystore, password, client secret, tokens, contraseña de backup y secretos TOTP.
