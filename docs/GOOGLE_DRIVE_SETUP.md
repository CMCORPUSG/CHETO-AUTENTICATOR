# Google Drive — configuración

CHETO usa Google Identity Authorization y Google Drive API para una copia cifrada visible.

## Scopes

    https://www.googleapis.com/auth/drive.file
    https://www.googleapis.com/auth/drive.appdata

drive.file es el scope principal de la copia visible. drive.appdata se conserva para compatibilidad y migración. No se requiere acceso total a todo Drive.

## Ruta

    Google Drive
    └── Mi unidad
        └── CHETO Authenticator
            └── Backups
                └── cheto_native_backup_current.cheto

## Configuración en Google Cloud

1. Crear o seleccionar un proyecto.
2. Habilitar Google Drive API.
3. Configurar Google Auth Platform.
4. Configurar audiencia y usuarios de prueba si está en Testing.
5. Agregar drive.file y drive.appdata.
6. Crear cliente OAuth Android para com.cmcorpusg.chetoauthenticator.
7. Registrar el SHA-1 del certificado que firma el APK instalado.

Debug y release usan certificados distintos; normalmente se crean clientes Android separados.

## Obtener SHA-1 release

    & "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore ".\signing\cheto-release.jks" -alias "cheto-release"

No publicar la contraseña.

## Sign in with Google

Puede requerir un Web OAuth client ID. Configurarlo localmente:

    CHETO_GOOGLE_WEB_CLIENT_ID=TU_CLIENT_ID.apps.googleusercontent.com

Ubicación recomendada en Windows:

    %USERPROFILE%\.gradle\gradle.properties

No usar client_secret en Android.

## Backup automático

1. genera contenido portable;
2. calcula SHA-256;
3. sin cambios termina localmente;
4. con cambios obtiene autorización si corresponde;
5. comprime GZIP;
6. cifra AES-256-GCM;
7. actualiza cheto_native_backup_current.cheto.

## Problema típico: "Autorización cancelada" en release

Comprobar primero que exista un cliente OAuth Android Release con package y SHA-1 exactos de la firma release. Tener solo el cliente Debug no autoriza automáticamente un APK release con otra firma.

También revisar usuario de prueba, Drive API y scopes.
