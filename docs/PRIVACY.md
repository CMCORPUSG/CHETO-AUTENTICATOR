# Privacidad y tratamiento de datos — CHETO 1.0.0

## Datos locales

CHETO puede almacenar secretos TOTP, etiquetas, parámetros TOTP, categorías, favoritos, notas, papelera, perfil local, preferencias y metadatos mínimos de identidad o backup.

La bóveda sensible se protege con criptografía respaldada por Android Keystore.

## PIN y biometría

El PIN no se guarda como texto plano. La biometría la procesa Android; CHETO no recibe ni guarda plantillas biométricas.

## TOTP

La generación TOTP es local y puede funcionar offline.

## Backup .cheto

El formato actual serializa, comprime GZIP, cifra AES-GCM y guarda un contenedor .cheto. La contraseña de recuperación no se incluye dentro del archivo.

## Google Drive

Scopes: drive.file y drive.appdata.

Ruta:

    Mi unidad/CHETO Authenticator/Backups/cheto_native_backup_current.cheto

## OneDrive

Permisos: Files.ReadWrite.AppFolder y User.Read.

Ruta:

    Apps/CHETO Authenticator/cheto_native_backup_current.cheto

## Eficiencia cloud

Antes de autorización o token, el backup automático calcula SHA-256 local. Sin cambios no se sube nada ni se consume API en ese ciclo. Con cambios se reemplaza la copia actual.

## Tokens

No deben registrarse ni subirse a GitHub.

## Analítica

CHETO no requiere publicidad ni analítica conductual para generar TOTP.

## Eliminación

Desinstalar o borrar datos elimina la bóveda local de esa instalación. Los backups externos permanecen donde el usuario los guardó.
