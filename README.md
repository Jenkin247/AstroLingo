# AstroLingo

The language of the stars: a Duolingo-style app for learning Vedic astrology, with a daily guidance card.
Android first (Kotlin, Jetpack Compose); iOS later.

## Repository layout
| Path | What it is |
| --- | --- |
| `app/` | Android app |
| `engine/` | Pure-Kotlin jyotish engine (kundli maths, later daily-card rules) |
| `docs/adr/` | Architecture decision records |
| `.github/workflows/` | CI: tests, lint and a debug APK on every pull request |

## Build
- Engine tests: `./gradlew -p engine test`
- App: `./gradlew :app:assembleDebug` (needs the Android SDK, as installed by Android Studio)

## Install the latest build on your phone
Open the pull request, go to its **Checks** tab, open the CI run and download
`astrolingo-debug-apk` under Artifacts. Unzip it and open the `.apk` on the phone
(allow installs from your browser or file manager when asked).

## Standing rule: privacy and security by default
Birth data stays encrypted on the phone, never goes to analytics, ads or crash reports,
and is excluded from Android backup. See the architecture board in the MVP1 plan.
