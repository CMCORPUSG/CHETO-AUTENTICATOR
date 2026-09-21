# Abrir e instalar CHETO Android nativo

## Instalar en el teléfono

1. Copiar `CHETO-ANDROID-NATIVO.apk` a un teléfono con Android 8.0 o superior.
2. Abrir el archivo desde Descargas y permitir la instalación desde ese gestor
   de archivos cuando Android lo solicite.
3. Abrir CHETO Authenticator, crear el perfil local y elegir un PIN de seis dígitos.
4. Agregar una cuenta con el botón +, la cámara QR o una imagen QR.

Es un APK de desarrollo firmado, instalable directamente. No requiere un
servidor, navegador ni Android Studio para usarlo.

## Abrir el código fuente

Extraer `CHETO-ANDROID-CODIGO-FUENTE.zip` y abrir en Android Studio la carpeta
que contiene `settings.gradle.kts`. No abrir un archivo HTML.

La aplicación actual usa exclusivamente vistas nativas de Jetpack Compose.
La actividad principal está declarada en `app/src/main/AndroidManifest.xml`:

- `NativeActivity.kt`: ciclo de vida, PIN, biometría, cámara, archivos y Drive.
- `ui/NativeApp.kt`: registro, cuentas, editor, categorías, perfil y ajustes.
- `data/NativeVault.kt`: almacén AES-GCM/Keystore y archivos cifrados.
- `core/TotpEngine.kt`: generación TOTP offline.

Si Android Studio muestra `DirectoryLock / CannotActivateException`, el IDE
no ha conseguido iniciar su instancia. Eso es independiente de instalar el APK.
Cerrar otras instancias de Studio y reintentar. No es necesario restablecer los
ajustes ni borrar el proyecto. La compilación por Gradle funciona sin el IDE:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Se requieren JDK 17, Android SDK y `local.properties` con `sdk.dir` apuntando al
SDK de cada equipo. El ZIP no incluye rutas locales, credenciales ni cachés.

## Respaldos

La exportación y restauración local `.cheto` conserva cuentas, categorías,
notas, fotos y perfil. La contraseña de recuperación no se guarda y el PIN
local no se incluye en la copia. Se mantiene compatibilidad con los archivos
`.cheto` de la versión HTML anterior.

Drive usa OAuth real y requiere registrar en Google el identificador
`com.cmcorpusg.chetoauthenticator` y el SHA-1 del certificado del APK. Sin esa
configuración, usar el respaldo local. Esta entrega realiza copias manuales;
no programa respaldos automáticos del almacén nuevo.

El cambio desde la versión anterior conserva su contenedor cifrado y sus
cuentas. No desinstalar antes de actualizar si se quieren conservar los datos.
Una firma de APK distinta requiere respaldar antes de reinstalar.
