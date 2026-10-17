# ADR 0009: Crashlytics with personal data stripped, no analytics in MVP1

- Status: accepted
- Date: 2026-10-17

## Context
We need crash reports during user testing. We must never send birth data, and the PO chose no analytics for MVP1.

## Decision
- Use Firebase Crashlytics without the Analytics SDK, and set `firebase_analytics_collection_deactivated=true`.
- Set no custom keys or logs that hold user data. Never call `setUserId` with the Firebase UID; use a random per-install id instead.
- Before upload, strip exception messages from our own exception types. Birth data classes override `toString()` (ADR 0002).
- Tester feedback goes through a "Send feedback" form in Me, not through analytics.

## Consequences
- We'll see crashes and stack traces, but not usage funnels. Usage questions get answered in user testing.
