# Account module

Google sign-in and the user's choice of updates (ADR 0012). Android library, used by `:app`.

## Turning sign-in on

Sign-in stays off until the Firebase project exists. Once it does, add these four lines to the root `gradle.properties`
(they are public identifiers that ship in every APK, not secrets):

```
astrolingo.firebase.webClientId=<OAuth 2.0 "Web client" id, from Firebase > Authentication > Sign-in method > Google>
astrolingo.firebase.apiKey=<"current_key" in google-services.json>
astrolingo.firebase.appId=<"mobilesdk_app_id" in google-services.json>
astrolingo.firebase.projectId=<"project_id" in google-services.json>
```

Firebase also needs the SHA-1 of each signing key (debug and Play) under Project settings > Your apps, or Google
returns no account.

## Tests

`./gradlew :account:testDebugUnitTest`. The app's journeys use `FakeAccountRepository` (app/src/test/.../e2e/SignInJourneys.kt).
