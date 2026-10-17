# ADR 0006: Lessons, card messages and Sitara guardrails as versioned JSON content

- Status: accepted
- Date: 2026-10-17

## Context
The Guru writes lessons and about 150 daily card messages. The Sitara guardrails change as testing teaches us. Hindi follows after MVP1. None of this should need a code change to edit.

## Decision
- Content lives in `app/src/main/assets/content/` as JSON, one file per unit and one for card messages. Each file has a `schemaVersion` and a `contentVersion`.
- Card messages are keyed exactly as `docs/spec/daily-card-rules.md` describes: theme by house and strength, star line by tara, day line by vara, and Rahu kaal tip by strength.
- Every user-facing string has an `en` value now and room for `hi`.
- A unit test loads every content file and checks it against its schema, that every card key has at least one message, and that no message is over 140 characters.
- The Sitara guardrail prompt lives with the Cloud Function, versioned in the repo. It isn't shipped in the app.

## Consequences
- The Guru can edit content through pull requests. CI catches broken files.
- Remote content updates (without an app release) are left for MVP2.
