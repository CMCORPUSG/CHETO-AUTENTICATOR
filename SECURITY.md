# Security policy

CHETO-AUTENTICATOR is designed so that GitHub contains source code only.

Never commit:

- TOTP secrets or QR payloads.
- exported authenticator backups.
- recovery passwords.
- OAuth client secrets.
- signing keystores or passwords.
- real access tokens.

Backups are encrypted before upload. Local account data is encrypted with a key generated in Android Keystore.

For a security issue, avoid opening a public issue containing secrets, QR codes, tokens or screenshots of live codes.
