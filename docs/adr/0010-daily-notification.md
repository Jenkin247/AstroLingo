# ADR 0010: Daily notification with WorkManager, no exact-alarm permission

- Status: accepted
- Date: 2026-10-17

## Context
Users choose a morning time for their daily card. Exact alarms need the `SCHEDULE_EXACT_ALARM` permission, which Play restricts.

## Decision
- A `PeriodicWorkRequest` with a 24-hour period, aligned to the chosen time by its initial delay and rescheduled whenever the time changes or the time zone changes.
- The worker computes the card on the phone (engine and content) and posts the notification. Nothing goes to a server.
- Ask for `POST_NOTIFICATIONS` on Android 13 and up, at the moment the user picks a time, with a short explanation.

## Consequences
- The notification can arrive a few minutes late. That's fine for a morning card.
- WorkManager survives reboots and app updates. QA checks it after a reboot and with Doze on.
