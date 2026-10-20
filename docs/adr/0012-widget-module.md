# ADR 0012: Home-screen widget as its own Android library module

- Status: proposed
- Date: 2026-10-20

## Context
The PO wants a small home-screen widget, like a weather widget, for the main user only: the planet of the day as an
icon that changes with the day (a Saturn widget on Saturday, a Mars widget on Tuesday), a circle in the colour of the
day, and one word for the mood of the hour (calm, energising). The app should offer it once after a day or two of use,
with a small preview. The PO asked for development module by module (widget, notifications, sign-in).

## Options considered
1. Widget code inside `app`. Simplest, but mixes it with the screens and goes against the module-wise ask.
2. Jetpack Glance in a new module. Compose-like, but a new library family for one small layout.
3. A new Android library module `:widget` with plain RemoteViews, depending only on the engine and Compose (for the
   in-app offer). The app hands it the main user's place and language through one interface.

## Decision
Option 3.
- `:widget` (`com.astrolingo.widget`): `AstroDayWidget` (the receiver), `WidgetDay` (pure content, JVM-tested),
  `WidgetViews` (RemoteViews), `WidgetOffer` (once, two days after first use, with a live preview and the launcher's
  "add widget" request), its own strings in English and Hindi.
- The mood of the hour comes from the hora (planetary hour) lord, worked out in the engine (`HoraCalculator`): the
  Vedic day from sunrise is split into 12 day and 12 night parts, ruled in the Chaldean order starting with the day lord.
- The app's `AstroLingoApplication` implements `AstroWidgetHost`: language tag and the main user's Today place. Saved
  friends never reach the widget. No birth data is shown; the place stays on the phone.
- No fixed update period. After each draw the widget sets one inexact, non-waking alarm for the next hora or midnight,
  and redraws on clock, time zone and language changes and when the app goes to the background. No new permissions.

## Consequences
- The widget shows the hour's mood only once the user is onboarded; before that it shows the planet and colour.
- A widget update opens the encrypted profile store from a background thread (a few milliseconds, about 24 times a day).
- Mood words are a first pass by the developer from common hora meanings; the Astro Guru should review them.
