# ADR 0007: Motion with Compose animations; Lottie only for Sitara

- Status: accepted
- Date: 2026-10-17

## Context
The UX plan has a few motion moments: the kundli reveal, the card flip, the streak, and Sitara's presence. Animation libraries add size and work.

## Decision
- Use Compose's built-in animation APIs (`animate*AsState`, `AnimatedVisibility`, `updateTransition`, `Animatable`) for everything except Sitara.
- Use Lottie only for Sitara's idle and "thinking" loops, with files under 100 KB each.
- Respect the system "remove animations" setting: when it's on, motion is replaced by a fade.
- Motion pass (19 Oct 2026, PO picked "Compose first"): shared pieces live in `ui/components/Motion.kt`: a twinkling
  star field behind Today and the Learn path, a staggered rise as Today opens, a star burst on a right answer, and
  haptic taps (right, wrong, card flip, lesson done). Lesson cards tilt like a deck as they move and land on a soft
  spring; flip cards turn on a spring. Infinite motion only redraws, it never recomposes the screen.
- Tests render with animations off: an endless twinkle never lets a test clock go idle.

## Consequences
- One extra dependency (Lottie). Everything else stays in Compose.
- QA checks every motion moment with animations off.
