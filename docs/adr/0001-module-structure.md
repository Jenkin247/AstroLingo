# ADR 0001: Module structure

- Status: accepted
- Date: 2026-10-16

## Context
AstroLingo starts on Android and moves to iOS later. The kundli maths and daily-card rules
must be identical on both, testable without a phone, and free of Android imports.

## Options considered
1. One Android module for everything. Fastest to start, but the astrology logic would be
   tangled with Android code and hard to reuse on iOS.
2. An Android app plus a pure-Kotlin engine built separately. Slightly more setup.
3. Kotlin Multiplatform from day one. Most reuse, but more build complexity before we need it.

## Decision
Option 2. The repository has:
- `app/`: the Android app (Kotlin, Jetpack Compose, Material 3). Feature code will be split
  into `feature:*` and `core:*` modules as it grows.
- `engine/`: the jyotish engine, a standalone pure-Kotlin build included into the app build
  with `includeBuild`. It can be built and tested alone with `./gradlew -p engine test`.

## Consequences
- The engine can become a Kotlin Multiplatform module for iOS without moving code.
- Astrology rules are unit tested on a plain JVM, which is fast in CI.
- The app depends on the engine as `com.astrolingo:jyotish`, resolved from the included build.
