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

## Addendum (2026-10-19): more natural voices
The PO found the default voice too robotic. Still free and on-device:
- Use Google's speech engine when the phone has it (most Android phones do; Samsung phones default to Samsung's own engine).
- Choose the voice separately for English and Hindi: the highest-quality installed voice for that language, same country first (`en-IN`, `hi-IN`).
- Never a voice that needs the network, because "Your chart" cards speak the learner's own placements.
- Next options if this is still not enough: pre-recorded audio for the fixed course text, made once with a neural voice; or a cloud voice (paid above its free tier).
