# Google Drive backup setup

CHETO Authenticator uses the Google Drive **appDataFolder** scope:

`https://www.googleapis.com/auth/drive.appdata`

This scope lets the app create and read only its own hidden application data. It does not request access to the user's entire Drive.

## One-time Google Cloud configuration

1. Create or select a Google Cloud project.
2. Enable **Google Drive API**.
3. Configure the OAuth consent screen.
4. Add the non-sensitive Drive scope:
   `https://www.googleapis.com/auth/drive.appdata`.
5. Create an **Android OAuth client** for:
   - Package: `com.cmcorpusg.chetoauthenticator`
   - SHA-1: use the signing certificate SHA-1 for the build you install.
6. For local development, register the debug SHA-1.
7. For the final APK, register the release signing certificate SHA-1.

The client-side Google Identity authorization flow does not require storing a client secret inside this repository.

## Backup behavior

- TOTP secrets remain encrypted on the phone.
- Before upload, the portable backup is encrypted with AES-256-GCM.
- The backup key is derived from the recovery password using PBKDF2-HMAC-SHA256.
- The recovery password itself is never uploaded.
- The phone stores only a wrapped derived recovery key, protected by Android Keystore.
- WorkManager requests a backup every 24 hours when network is available.
- A backup is also requested after account changes.
- The Drive folder retains the latest 7 encrypted backup files.

## Restore

On a new Android phone:

1. Install CHETO Authenticator signed/configured for the same Google project.
2. Tap **Restaurar desde Google Drive**.
3. Authorize the Google account containing the app backup.
4. Enter the recovery password.
5. The app downloads the newest encrypted backup and decrypts it locally.

Without the recovery password, the Drive file is not sufficient to reconstruct the TOTP secrets.
