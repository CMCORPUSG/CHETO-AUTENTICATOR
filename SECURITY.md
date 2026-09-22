# Security Policy

CHETO Authenticator trata TOTP, backups y claves de firma como material sensible.

## Nunca subir

- secretos TOTP u otpauth reales;
- QR o códigos 2FA reales;
- archivos .cheto;
- contraseñas de recuperación;
- access o refresh tokens;
- client secrets;
- keystores;
- passwords de keystore;
- keystore.properties real;
- archivos de entorno con secretos.

## Cifrado

La bóveda local usa criptografía respaldada por Android Keystore. Los backups se comprimen y cifran antes de salir del dispositivo.

## OAuth

Client IDs, redirects y fingerprints son identificadores públicos. No son client secrets, tokens ni claves privadas.

Un fork debe registrar su propia configuración OAuth y firma cuando corresponda.

## Firma Android

El keystore release debe mantenerse fuera de Git y respaldado de forma segura.

## Reportes

No abrir issues públicos con secretos, QR, tokens, passwords o backups reales.

## Verificación

    git status --short
    git check-ignore -v ".\keystore.properties"
    git check-ignore -v ".\signing\cheto-release.jks"

El CI incluye un guard básico de secretos.
