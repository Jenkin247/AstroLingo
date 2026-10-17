# ADR 0008: Read-aloud with Android's on-device text-to-speech

- Status: accepted
- Date: 2026-10-17

## Context
The PO asked for a voice for the AI. MVP1 has a speaker button on the card, the "Why?" sheet and Sitara's answers.

## Options considered
1. A cloud voice (for example Google Cloud Text-to-Speech). Better voices, but it costs money per character and sends text off the phone.
2. Android `TextToSpeech`, which runs on the device.

## Decision
Option 2.
- Prefer an Indian English voice (`en-IN`) when one is installed, and fall back to the default.
- Speak at the system rate.
- Stop speaking when the screen changes.

## Consequences
- Free, offline and private. Voice quality depends on the phone.
- A branded Sitara voice is a candidate for MVP2.
