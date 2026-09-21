# CHETO Authenticator — Identity setup

CHETO keeps TOTP generation local. Google and Microsoft sign-in are optional identity features and do not replace the local PIN/biometric protection.

## Google

CHETO uses Android Credential Manager + Sign in with Google.

1. In Google Auth Platform, create/configure the project.
2. Register the Android package:
   `com.cmcorpusg.chetoauthenticator`
3. Add the SHA-1 of the signing certificate used by the APK.
4. Create the Web OAuth client ID required by Sign in with Google.
5. Put the public Web client ID in your local Gradle properties, not in source code:

```properties
CHETO_GOOGLE_WEB_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com
```

Recommended location on Windows:

```text
%USERPROFILE%\.gradle\gradle.properties
```

Do not add a Google client secret to an Android application.

CHETO does not persist the Google ID token. It stores only provider metadata such as account ID, email, display name and photo URI. If CHETO later uses provider identity for server-side authorization, the ID token must be validated by a trusted backend.

## Microsoft

CHETO uses Microsoft Authentication Library (MSAL) in single-account mode.

1. Create an app registration in Microsoft Entra.
2. Select the account audience required by CHETO. The current template supports work/school and personal Microsoft accounts.
3. Add the Android platform using package:
   `com.cmcorpusg.chetoauthenticator`
4. Generate/register the signature hash for the certificate used by the APK.
5. Copy the public Application (client) ID to local Gradle properties:

```properties
CHETO_MICROSOFT_CLIENT_ID=00000000-0000-0000-0000-000000000000
```

6. Replace the placeholder values in:

```text
app/src/main/res/raw/auth_config_single_account.json
```

with the MSAL Android configuration returned by Microsoft Entra, especially `client_id` and `redirect_uri`.

Never place a Microsoft client secret in the Android app.

## Debug certificate

To inspect the debug certificate locally:

```powershell
keytool -list -v -alias androiddebugkey -keystore "$env:USERPROFILE\.android\debug.keystore" -storepass android -keypass android
```

Google and Microsoft must be configured for the certificate that signs the APK being tested. A future release certificate has a different fingerprint/signature and must be registered separately.
