# ADR 0003: Cloud sync with Tink, Block Store and Firestore EU

- Status: accepted
- Date: 2026-10-17

## Context
Sign-in is required (PO decision), so a user who changes phones expects their profile back. Our servers must never be able to read it.

## Options considered
1. Firestore with Google's encryption at rest only. Simple, but we and Google could read the profile.
2. Client-side encryption, with the key escrowed on our server. Readable by whoever holds the server.
3. Client-side encryption with Tink, with the data key kept in Android Block Store, which is end-to-end encrypted by Google and tied to the user's screen lock.

## Decision
Option 3.
- A per-user Tink AES-256-GCM data key encrypts the serialised profile. The associated data is the Firebase UID, so a ciphertext can't be moved to another account.
- Only the ciphertext and a schema version go to Firestore, in an EU region (`europe-west`). Security rules allow read and write only by the owning UID.
- The data key goes to Block Store with `shouldBackupToCloud=true`. Our backend never sees it.
- On a new phone: sign in, fetch the ciphertext, read the key from Block Store and decrypt. If there is no key, the user re-enters their birth details (PO decision 5), and the old ciphertext is overwritten.
- Delete everything removes, in order, the Firestore document, the Block Store key, the Firebase account and the local database. It retries when offline.

## Consequences
- Restore depends on the user having Google backup and a screen lock. The sprint 1 spike on the PO's Samsung proves this before sign-in is built in sprint 3.
- Users in India store ciphertext in the EU. That is fine under DPDP rules for this data, because it is unreadable without the key on the user's devices.
