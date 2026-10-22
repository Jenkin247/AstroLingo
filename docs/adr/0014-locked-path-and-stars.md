# ADR 0014: Lessons open in order, and Stars kept with learning progress

- Status: proposed
- Date: 2026-10-22
- Changes: the learn module design (`docs/design/learn-module.md`, open question 1: every chapter was open)

## Context
The PO wants learners to go through the course in order and prove each step, and wants daily visits to matter
(22 Oct 2026). Lessons should open only after the one before is passed, and learners should earn points, called Stars
for now, for coming back and for learning. Later, Stars turn into extra questions for Sitara (proposal doc "AstroLingo
launch, learning path and AI credits proposal"). The name Stars may change.

## Decision
- **Locked path in the learn engine.** `CoursePath` works out which lessons are open (`unlocked`) and a new
  `ChapterState.LOCKED`. A chapter opens when every chapter before it is done or known; inside an open chapter a
  lesson opens when the one before it is passed. Chapters passed in the knowledge check count as done and are open in
  full, so experienced learners still skip what they know. Lessons finished before this change stay open, with the
  lesson after them: nobody loses progress.
- **Pass mark.** A lesson is passed with two thirds of its quiz right (`QuizPass`, 2 of 3), with unlimited retries.
  Fewer right: the done card offers the quiz again and no next lesson. A lesson peeked at from Today before its turn
  can be read but doesn't count.
- **Stars** (`com.astrolingo.learn.stars.Stars`) live inside `LearnProgress`, saved by the same codec (version 3), so
  a lesson pass and its Stars are saved together. Rules: first visit of the day 2, first pass of a lesson 3, plus 1 for
  a perfect quiz, finished Review 1 a day, at most 9 a day. Stars are kept on the phone only, are not birth data and are
  wiped with "Delete my details" like the rest of learning progress.
- **The day's visit** is counted where progress is already loaded: by Learn when the app opens there, otherwise by the
  app shell on start and on each return to the app, so the two never overwrite each other's progress.
- **Renaming.** Code always says `stars`. The name people see comes only from `stars_name` in
  `res/values*/strings_stars.xml`; every other Stars line takes it as an argument.

## Consequences
- Stars can be changed by changing the phone's clock or data; that's acceptable while they buy nothing. When they turn
  into Sitara questions, the count that matters moves to the server with the account (proposal, AI section).
- No streak yet; the 7-day streak bonus in the proposal is a follow-up.
