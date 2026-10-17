# ADR 0002: Local encryption with SQLCipher and Android Keystore

- Status: accepted
- Date: 2026-10-17

## Context
Birth date, time and place, the kundli and the deity choice are sensitive. Some of them can reveal religion. They must be unreadable to other apps, to someone holding a copied phone image, and to Android's own cloud backup.

## Options considered
1. Room on plain SQLite, relying on Android's file-based encryption. The data is readable on a rooted or unlocked device, and it is easy to leak through backups.
2. Jetpack Security (EncryptedSharedPreferences or EncryptedFile). It is deprecated, and it doesn't fit relational data.
3. Room on SQLCipher (AES-256), with the database key wrapped by an Android Keystore key.

## Decision
Option 3.
- At first launch, generate a random 256-bit database passphrase.
- Wrap the passphrase with an AES-256-GCM key in Android Keystore, hardware-backed where the phone supports it (StrongBox, otherwise TEE). Store only the wrapped passphrase, in app-private storage.
- Exclude all app data from auto-backup and device-to-device transfer. This is already set in the manifest (`allowBackup=false` plus extraction rules).
- Never log birth fields. Data classes holding them override `toString()`.

## Consequences
- SQLCipher adds a few MB to the APK. Accepted.
- If the Keystore key is lost (for example after a factory reset), the local database is unrecoverable. The user restores from sync (ADR 0003) or re-enters their birth details.
- QA checks that the database file has no plaintext birth fields, and that `adb backup` gets nothing.

## Update, sprint 2
The first stored data is a single profile, so it lives in one key/value table opened directly through SQLCipher (`EncryptedProfileStore`), with no Room yet. Room on the same SQLCipher database is added when lesson progress needs real tables. The rest of this decision is unchanged.
