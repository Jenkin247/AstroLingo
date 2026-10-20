# ADR 0012: Sign-in in its own module, optional, with an opt-in for updates

- Status: accepted
- Date: 2026-10-20
- Changes: ADR 0004 (sign-in screen had no skip)

## Context
The PO asked for Google sign-in in a separate module, and after sign-in a question on whether the user wants offers,
lessons, insights or a newsletter (20 Oct 2026). The sign-in analysis of 19 Oct recommended a "Maybe later" skip: the
kundli, Today and lessons all work on the phone, so a required sign-in mostly loses people at the door. The Firebase
project does not exist yet.

## Decision
- A new Android library module, `:account` (package `com.astrolingo.account`), holds sign-in, the updates choice and
  their screens. The app sees only `AccountRepository`, `AccountState` and `UpdatesConsent`, never Firebase types.
- Sign-in follows ADR 0004: Credential Manager's Sign in with Google, then Firebase Auth. No extra Google scopes; only the
  Firebase uid is kept.
- Sign-in is offered once, right after onboarding (or on the first launch of this version), with "Continue with Google"
  and "Maybe later". Afterwards it lives in Me. Everything except backup and Sitara works without it.
- After a sign-in from that offer, the user is asked about four updates: new lessons, sky insights and offers as
  notifications, and a newsletter by email. All start off. "Save my choice" and "Not now" both record the choice with
  its time, as the record of consent. The choice can be changed in Me. Signing out turns everything off.
- "Delete my birth details" also signs out and forgets the choice, so sign-in is offered again after onboarding.
- Firebase settings come from `gradle.properties` (`astrolingo.firebase.webClientId`, `apiKey`, `appId`,
  `projectId`), not from `google-services.json` and its plugin. With them empty the app runs as before and "Continue
  with Google" says sign-in isn't switched on in this version.

## Consequences
- The updates choice is stored only on the phone for now. Sending a newsletter needs the email address and a mail
  service, and the choice must reach our backend with the uid; both wait for the Firebase project and a privacy policy
  update.
- The notification engine must check `UpdatesConsent` before sending anything beyond the user's own daily reminder.
- People who skip have no account yet. An anonymous Firebase account for them comes with the active-user check-in.
