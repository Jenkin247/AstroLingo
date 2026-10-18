# learn

The Learn module's logic and course content, in pure Kotlin with no Android imports (ADR 0011).
Design: `docs/design/learn-module.md`. Content format and writing rules: `docs/spec/learn-content.md`.

## Public API
- `CourseLoader.bundled()`: the course from `src/main/resources/content/learn/`. Load once, off the main thread.
- `Course`, `Chapter`, `Lesson` and the cards: `LearnCard`, `YourChartCard` (filled from the user's kundli by the app), `QuizCard`.
  Text is `LocalizedText`, read with `get(Language)`; Hindi falls back to English until written.
- `CoursePath.of(course, progress)`: each chapter's state (known, done, in progress, not started) and the next lesson.
- `KnowledgeCheck.of(course, chapterIds)`: the questions for chapters the learner says they know; `passedChapters()`
  are the ones to skip.
- `LearnProgress`: lessons done, known chapters, bookmarks and the resume point. `LearnProgressCodec` stores it as
  key/value text.

## Testing
```
./gradlew -p learn test
```
Logic tests use a small made-up course (`Fixtures`). Content tests load the real course and enforce every rule in
the content spec, so a broken or over-long card fails CI. Writers can run `python3 tools/learn/check_content.py`
for a quick check without Gradle.
