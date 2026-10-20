# ADR 0010: Daily notification with WorkManager, no exact-alarm permission

- Status: accepted
- Date: 2026-10-17, updated 2026-10-20

## Context
Users choose a morning time for their daily card. Exact alarms need the `SCHEDULE_EXACT_ALARM` permission, which Play restricts.

## Decision
- A `PeriodicWorkRequest` with a 24-hour period, aligned to the chosen time by its initial delay and rescheduled whenever the time changes or the time zone changes.
- The worker computes the card on the phone (engine and content) and posts the notification. Nothing goes to a server.
- Ask for `POST_NOTIFICATIONS` on Android 13 and up, at the moment the user picks a time, with a short explanation.

## Consequences
- The notification can arrive a few minutes late. That's fine for a morning card.
- WorkManager survives reboots and app updates. QA checks it after a reboot and with Doze on.

## Update, 20 October 2026: the notification engine

- The engine is its own Android library module, `notify/`, with no astrology in it: the app hands it each note's
  words through a `NoteContent` interface on its Application. Design: docs/design/notifications.md.
- Still a 24-hour `PeriodicWorkRequest` aligned by its initial delay. Changing the time enqueues a fresh job
  (`CANCEL_AND_REENQUEUE`), because WorkManager applies the initial delay only to a job's first run. A run more than
  20 minutes off the chosen time (summer time, a long sleep) sets the job again after it posts.
- Permission is asked when the user says yes to the note: on the offer card Today shows from their second day, or the
  switch in Me. Never on first launch.
- A weekly fallback (PO, 20 Oct): once a daily note goes unanswered, daily notes pause and one note about the week
  goes out each Monday until the app is opened again.
- Low-importance channel, private visibility with a public version that shows nothing from the chart.
