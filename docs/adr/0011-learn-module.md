# ADR 0011: Learn module as a pure-Kotlin build with its content

- Status: proposed
- Date: 2026-10-17

## Context
Learn is the core of the product: a course of 14 chapters taught in swipe-up cards, a knowledge check that skips
chapters the learner already knows, and bookmarks (design: `docs/design/learn-module.md`). The course logic and
several hundred cards of content must be testable without a phone, ready for Hindi, and reusable on iOS. Another
team is changing the Today and Kundli screens at the same time, so Learn must stay out of their files.

## Options considered
1. An Android `feature:learn` module. Matches the long-term module plan, but needs the theme and Sitara moved out of
   `app` first (a shared-code change the other team is in the middle of), and none of it can be tested off Android.
2. Everything inside `app`. Simple, but the content checks and course rules would need Robolectric.
3. A pure-Kotlin `learn/` build, included like `engine/`, for the content model, loader, path, knowledge check and
   progress, plus the Compose screens in their own `com.astrolingo.app.learn` package.

## Decision
Option 3.
- `learn/` (`com.astrolingo:learn`): no Android imports; tested with `./gradlew -p learn test`.
- Course content lives in `learn/src/main/resources/content/learn/` (format: `docs/spec/learn-content.md`). This keeps
  ADR 0006's rules (versioned JSON, `en` plus room for `hi`) but places the files beside the code that reads them, so
  every content edit is checked by the JVM tests in CI, as the city list is with the engine.
- JSON is parsed with `kotlinx-serialization-json` (Apache 2.0, Maven Central), the only new library.
- Progress and bookmarks are not birth data. The app keeps them in private storage, already excluded from backup and
  device transfer, and "Delete my details" wipes them with the profile.
- The UI reads the user's kundli only to fill "your chart" cards on screen. Nothing leaves the phone.

## Consequences
- The whole course can be checked on a plain JVM in seconds, and moves to Kotlin Multiplatform with the engine.
- When `core:ui` is split out of `app`, the Learn screens can move into `feature:learn` without touching `learn/`.
- Remote content updates (without an app release) stay in MVP2, as ADR 0006 says.
