# ADR 0004: Google-only sign-in with Firebase Auth and Credential Manager

- Status: accepted
- Date: 2026-10-17

## Context
The PO decided that sign-in is required and Google only for MVP1, with no phone OTP. Nearly all target users have a Google account on their Android phone.

## Decision
- Use Credential Manager's Sign in with Google (`GetGoogleIdOption`, falling back to `GetSignInWithGoogleOption`).
- Exchange the ID token for a Firebase Auth credential. The Firebase UID identifies the user to Firestore and the Sitara function.
- Request no Google scopes beyond the basic profile, and store no email or name in our database.
- The sign-in screen has no skip. Offline sign-in is queued and retried, and the kundli still works locally in the meantime.

## Consequences
- Users without a Google account can't use MVP1. Accepted for the closed test.
- App Check (Play Integrity) is required on both Firestore and the Sitara function (ADR 0005).
