# Architecture decision records

| ADR | Decision |
|---|---|
| [0001](0001-module-structure.md) | Android app plus a standalone pure-Kotlin jyotish engine |
| [0002](0002-local-encryption.md) | Local encryption with SQLCipher and Android Keystore |
| [0003](0003-cloud-sync.md) | Cloud sync with Tink, Block Store and Firestore EU |
| [0004](0004-sign-in.md) | Google-only sign-in with Firebase Auth and Credential Manager |
| [0005](0005-ask-sitara.md) | Ask Sitara through a Cloud Function, no personal data, daily cap |
| [0006](0006-content-as-json.md) | Lessons, card messages and guardrails as versioned JSON content |
| [0007](0007-motion.md) | Motion with Compose animations; Lottie only for Sitara |
| [0008](0008-read-aloud.md) | Read-aloud with on-device text-to-speech |
| [0009](0009-crash-reporting.md) | Crashlytics with personal data stripped, no analytics |
| [0010](0010-daily-notification.md) | Daily notification with WorkManager, no exact alarms, in the `notify` module |
| [0011](0011-learn-module.md) | Learn module as a pure-Kotlin build with its content |
| [0012](0012-account-module.md) | Sign-in in its own module, optional, with an opt-in for updates |
| [0013](0013-widget-module.md) | Home-screen widget as its own Android library module |
| [0014](0014-locked-path-and-stars.md) | Lessons open in order, and Stars kept with learning progress |

A change to modules, the data model, security or a new library needs a new or updated ADR, reviewed by the Architect.
