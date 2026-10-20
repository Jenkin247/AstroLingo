# Notification engine

The daily note: one calm notification each morning about the user's day, and a weekly note instead when they have
been away. Built as its own module, `notify/`, so the widget and sign-in work can sit beside it later (ADR 0010).

## What the PO asked for (19 and 20 Oct 2026)

- Ask the user before notifying them. Not bothersome: once a day at most, in the morning.
- The day in one line, the colour of the day and the deity to worship, so they know how to start their day; the app
  has the rest.
- A calm planet picture in the notification.
- If the user doesn't come back after a day, stop the daily notes and send one note about the week ahead instead.
- Look at how other apps do it for ideas, without copying their words.

## What other apps do (for reference only)

From public knowledge of these apps as of 2026, not checked on a phone for this work. No wording was taken from any
of them; every line in AstroLingo's notes was written for AstroLingo.

| App type | What they do | What we take from it |
|---|---|---|
| Western astrology apps (Co-Star, The Pattern, Nebula) | One short daily push about "your day", often at a time the user picks | One note a day, short, the user picks the time |
| Indian astrology marketplaces (Astrotalk, AstroSage) | Several promotional pushes a day (offers, live astrologers) | What we avoid: the PO wants calm, so no offers in this channel (marketing is a separate opt-in with sign-in) |
| Language apps (Duolingo) | A daily reminder that backs off and stops when ignored for some days | Our weekly fallback: an unanswered note pauses the daily ones |
| Meditation apps (Headspace, Calm) | Ask for reminders after the first session, user-chosen time, quiet tone | Ask on the second day, from inside the app, not on first launch |
| Weather apps | A rich notification with a picture that fits the day | The planet picture of the weekday |

Android's own guidance agrees: ask for `POST_NOTIFICATIONS` in context, and use a low-importance channel for
something that isn't urgent.

## Rules

- **Asking.** Never on first launch. From the second day, Today shows a small card: "A calm note each morning?" with
  "Yes, please" and "Not now". Yes asks Android for permission (Android 13+). Not now hides the card for good. Me has
  a switch and the time at all times.
- **When.** 7:30 in the phone's own time unless the user picks another. WorkManager runs once a day; a note that
  would arrive more than three hours late (the phone was off or asleep) is left out.
- **Once a day at most.** No note on a day the user already opened the app. Opening the app also clears the note.
- **Away.** If a daily note goes out and the app isn't opened before the next one is due, the daily notes stop. Each
  Monday, one note about the week ahead goes out instead. Opening the app brings the daily notes back the next day.
- **Quiet.** Low-importance channel: no sound, no vibration, no pop-up, no badge. One note at a time; a new one
  replaces the old.
- **Private.** Everything is worked out on the phone from the encrypted profile. A locked phone shows only
  "AstroLingo · Your day is ready". The deity is left out when the user turned deities off.

## Words

- Daily: title "A strong day: Begin what matters" (strength and focus line from the Today card's message library),
  text "Colour: Yellow · Deity: Vishnu". On a day with no sunrise (far north or south), the title is the weekday
  ("Sunday · Surya's day").
- Weekly: "A new week begins", then the week's best day by the card's rules: "Thursday looks like your strongest
  day. Take a look at the week ahead." A week with only gentle days says so kindly.
- English and Hindi, in the app's chosen language.

## Pictures

Drawn on the phone (`PlanetArt`), so there are no image files: a deep blue night sky, a few faint stars, and the
weekday's planet with a soft glow in colours close to the colour of the day. Saturn has its ring, Jupiter its bands,
the Moon a few craters. The same picture each week for a planet, so it feels familiar. Screenshots 53 to 55 show them.

## Code

- `notify/` (Android library, package `com.astrolingo.notify`): `NotePlan` (the rules, pure Kotlin),
  `DailyNotes` (settings and history in private preferences), `NoteScheduler` and `NoteWorker` (WorkManager),
  `NotePoster` (channel and notification), `PlanetArt` (pictures), `ClockReceiver` (time zone changes).
  It knows no astrology: the app gives it the words through `NoteContent`.
- `app/.../notes/`: `NoteWords` (the wording), `AppNotes` (reads the profile), `NotesUi` (offer and Me card).
  `AstroLingoApplication` implements `NoteContent`.

## Later

- The widget can reuse `PlanetArt` and the day's words.
- Marketing messages (offers, lessons, newsletters) need their own opt-in at sign-in and their own channel, never
  this one.
