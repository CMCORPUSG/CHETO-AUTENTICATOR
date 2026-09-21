# CHETO Authenticator — Privacy and data handling draft

This document describes the intended data behavior of CHETO Authenticator and is a release-preparation draft.

## Data stored on the device

CHETO may store:

- TOTP secrets and account labels.
- Categories, colors and notes.
- Local profile name, photo and email list.
- Linked Google/Microsoft identity metadata.
- Security preferences and backup status.

The local vault is encrypted with Android Keystore-backed cryptography. The PIN is persisted as a password verifier rather than as the original six digits.

## Biometric data

CHETO does not receive or store fingerprint templates or facial biometric templates. Enrollment and biometric matching are performed by Android. CHETO receives only the authentication result exposed by Android's biometric APIs.

## TOTP operation

TOTP generation is local and does not require internet access. CHETO must not transmit TOTP secrets or generated one-time codes to logo services, analytics providers or identity providers.

## Logos

When an online service logo is requested, CHETO may request a favicon using a service/domain identifier. TOTP secrets, generated codes, recovery passwords and private profile data must not be included in logo requests.

## Backups

A `.cheto` backup is encrypted before it is written to storage or uploaded to Google Drive. The recovery password is required to decrypt the portable backup.

Google Drive backup uses the app-private data area where configured. CHETO should not upload an unencrypted vault.

## Google and Microsoft identity

Provider sign-in is optional. CHETO stores only the provider identity metadata needed for the local profile. Provider access/ID tokens are not intentionally persisted inside the CHETO vault.

For any future server-side authorization based on Google or Microsoft identity, identity tokens must be validated by a trusted backend before they are treated as authoritative.

## Analytics and advertising

The current application does not require advertising SDKs or behavioral analytics to provide TOTP functionality.

## Deletion

Deleting application data removes the local CHETO vault from the device. External encrypted backups remain wherever the user stored them until the user deletes those copies.
