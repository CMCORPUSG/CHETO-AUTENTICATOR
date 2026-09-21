# CHETO Authenticator — Release preparation

## 1. Generate a release keystore

Keep the keystore outside Git or in a protected local folder.

Example:

```powershell
keytool -genkeypair -v -keystore D:\keys\cheto-release.jks -alias cheto -keyalg RSA -keysize 4096 -validity 10000
```

Do not commit the keystore or its passwords.

## 2. Create local keystore.properties

Copy `keystore.properties.example` to `keystore.properties` in the repository root and fill it locally:

```properties
storeFile=D:/keys/cheto-release.jks
storePassword=...
keyAlias=cheto
keyPassword=...
```

The real `keystore.properties` and `*.jks` files are ignored by Git.

## 3. Register the release certificate with identity providers

The release APK/AAB is signed by a different certificate from the Android debug certificate.

Before Google or Microsoft identity is expected to work in a release build:

- Register the release SHA-1/SHA-256 in Google Auth Platform.
- Register the Android signature hash/redirect URI in Microsoft Entra.
- Keep Android client secrets out of the app. Mobile apps are public clients.

## 4. Build candidate

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebugAndroidTest assembleRelease
```

If `keystore.properties` is present and valid, the release artifact is signed with the configured release key.

## 5. Validate on a physical device

Before distribution verify:

- PIN unlock and lockout.
- Biometric enrollment and unlock.
- Automatic lock.
- QR camera and gallery.
- Google Authenticator migration QR.
- Duplicate detection.
- Manual TOTP.
- Clipboard clearing.
- FLAG_SECURE / screenshots.
- Local `.cheto` export, replace restore and non-destructive merge restore.
- Selective encrypted export of chosen accounts.
- Recycle bin restore and permanent deletion.
- Forgot-PIN recovery from a valid encrypted `.cheto` backup.
- Clipboard timeout and protected code reveal.
- Automatic device time warning.
- Google Drive backup, replace restore and merge restore.
- Google identity linking.
- Microsoft identity linking.
- Restore on a second/clean device.

## 6. Distribution

For direct internal testing use the release APK. For Google Play use an Android App Bundle (AAB) and Play App Signing.

Never reuse the debug keystore as the production release identity.
