# Learn module: design

Owners: BA, Astro Guru, UX/UI (design meeting); Architect, Android Dev, QA (build meeting). Status: proposed to the PO, 17 Oct 2026.

## 1. Design meeting: BA, Astro Guru, UX/UI (QA listening)

### Goal
A learner opens Learn and is taught Vedic astrology from the very beginning (what Jyotish is and where it came from) to reading a kundli and beyond, in short swipe-up cards. Nobody is made to re-learn what they already know: a short check at the start skips whole chapters.

### How the classic books order the subject (inspiration only, no text copied)
| Book | Order, simplified |
|---|---|
| Brihat Parashara Hora Shastra (Parashara) | Creation and time → grahas → rashis → special points → vargas → bhavas → drishti → yogas → dashas → ashtakavarga → remedies |
| Brihat Jataka (Varahamihira, 6th c.) | Definitions of rashis → grahas and their nature → strength → yogas → dashas |
| Phaladeepika (Mantreswara, 14th c.) | Rashis → grahas → vargas → strength → bhavas → yogas → dashas → transits |
| Modern teaching books (B. V. Raman, K. N. Rao, de Fouw and Svoboda) | Why and where Jyotish came from → grahas → rashis → bhavas → nakshatras → reading a chart → dashas → transits |

The Guru's take: every classic starts with **the actors (grahas), the stage (rashis) and the scenes of life (bhavas)** before any prediction. Modern teachers add a first chapter on origins and on using astrology wisely, and bring nakshatras forward because dashas and the daily card depend on them. We follow that modern order, which is also the order in the Guru's syllabus.

### The course: 14 chapters
Each chapter has 3 to 5 lessons. A lesson is one idea, 3 to 5 minutes: 5 to 7 learning cards, then 3 quiz cards.

| # | Chapter | What the learner can do afterwards |
|---|---|---|
| 1 | Welcome to Jyotish | Say what Jyotish is, where it came from (Vedas, Vedanga Jyotisha, Parashara, Varahamihira), why the Vedic zodiac is sidereal, and how to use astrology wisely |
| 2 | The nine grahas | Name the nine grahas, what each signifies, benefic and malefic nature |
| 3 | The twelve rashis | Name the signs, their element, quality and lord |
| 4 | The twelve bhavas | Say what each house governs; kendra, trikona and dusthana groups |
| 5 | The 27 nakshatras | Explain nakshatras and padas, find their lords, know their own birth star |
| 6 | Reading a kundli | Read North and South Indian charts, find the lagna, placements and house lords |
| 7 | Drishti (aspects) | Apply the 7th-house aspect and the special aspects of Mars, Jupiter and Saturn |
| 8 | Dignity | Exaltation, debilitation, own sign, moolatrikona, friends and enemies, neecha bhanga |
| 9 | Vargas | What divisional charts are; the navamsa (D9); vargottama |
| 10 | Vimshottari dasha | The 120-year cycle, the order and years, how the first dasha comes from the Moon's nakshatra, antardashas |
| 11 | Gochar (transits) | Transits counted from the Moon; Jupiter, Saturn, Rahu and Ketu; sade sati in balance |
| 12 | Yogas | What a yoga is; Pancha Mahapurusha, raja and dhana yogas, Gaja Kesari; doshas without fear |
| 13 | Panchang and muhurta | The five limbs of the day, Rahu kaal, how the daily card is built |
| 14 | Kundli matching | Ashtakoota and the 36 gunas, and why a score is never the whole story |

### Cards
- **Graha flip card (PO, 17 Oct):** the card that introduces each graha shows a drawn picture of it on the front (Shani with its rings, Surya glowing, Rahu as the eclipse shadow, Ketu as a comet). A tap turns it over to the text; swipe up to go on.
- **Learn card:** a title, up to about 300 characters of text, an optional Sanskrit term (Devanagari and roman), and an optional visual: a big glyph, the zodiac wheel with one sign lit, the house diagram with one house lit, or the nakshatra ring with one star lit.
- **Your chart card:** the same lesson, made personal from the user's own kundli, for example "Your Moon is in Rohini, ruled by the Moon". When the birth time is unknown, cards that need the lagna or houses say so gently instead.
- **Quiz card:** a question, 3 or 4 answers, and a "why" explanation after answering. Wrong answers are never punished: the right answer and the reason are shown.
- **Lesson done card:** Sitara cheers, shows the score, and offers the next lesson.

### Adaptive start: skip what you know (PO request)
Onboarding already asks "How much do you know already?" (new / a few terms / can read a kundli).
1. **New to astrology:** start at chapter 1. A small link offers the check anyway.
2. **Knows a few terms / can read a kundli:** the first time Learn opens, Sitara asks **"Which of these do you already know?"** with a chip for each chapter (Grahas, Rashis, Bhavas, Dashas …).
3. For each chapter ticked, two quick questions. Both right: the chapter is marked **Known** and skipped. One or none right: the chapter stays on the path, and Sitara says it's a quick refresher.
4. The learner can stop the check at any time; anything not checked stays on the path.
5. Later, any chapter on the path has **"I know this"**, which runs the same two questions for that chapter only.
6. Known chapters stay open: the learner can always go back and read them.

Guru's note: the check asks about understanding (for example, "Which graha rules a Vimshottari period of 19 years?"), not trivia, so passing it really means the chapter can be skipped.

### Bookmarks
Every learn card has a bookmark star. **Saved** (top of the Learn tab) lists bookmarked cards, newest first, grouped by chapter. Tapping one opens the card; the star there removes it.

### Multilingual
English now, Hindi next. All lesson text is stored as `{ "en": "...", "hi": "..." }`; an empty Hindi value falls back to English. Screen labels are Android string resources, so Hindi is a `values-hi` file. Layouts allow 30% longer text and taller Devanagari lines.

### UX/UI: screens and motion
Learn uses the lavender tab colour and Sitara.
1. **Learn home (the path):** a vertical path of chapter tiles, each with its glyph, title, a progress ring and a state: *Up next*, *In progress*, *Done*, *Known*. The next lesson is a big "Continue" card at the top. A **Saved** button sits in the top bar.
2. **Chapter sheet:** opens from a tile with the chapter summary, its lessons with ticks, and "I know this".
3. **Card player:** full-screen cards, **swipe up for the next card** (vertical pager), a thin progress bar at the top, close at top left, bookmark star on each card. Cards scale and fade in as they arrive; the visual draws itself in; quiz answers pulse green or gently shake; the bookmark star pops; the lesson-done card drifts a few stars. With "Remove animations" on, every motion becomes a fade (ADR 0007).
4. **Knowledge check:** chips, then one question per screen with a progress bar, then "Your path is ready" showing what was skipped.
5. **Saved:** a list of cards, with an empty state ("Tap the star on any card to keep it here").

Accessibility: every card works at 200% font (text scrolls inside the card), all touch targets 48dp, TalkBack reads card title, body and "Card 3 of 9"; "Next card" is also a button for people who can't swipe.

## 2. Build meeting: BA, Architect, UX/UI, Android Dev, QA

### Architecture (ADR 0011)
- **`learn/`**: a new pure-Kotlin build, included like `engine/`, so the whole course logic and every content file are tested on a plain JVM. It holds the content model and loader, the course path (what's next, chapter states), the knowledge check, and learning progress with bookmarks.
- **Content** lives in `learn/src/main/resources/content/learn/` as one JSON file per chapter plus `course.json`. This follows ADR 0006 (versioned JSON, `en` plus room for `hi`), but sits with the learn module rather than in app assets, so CI validates it without Android, the same way the city list ships with the engine.
- **App UI** lives in its own package, `com.astrolingo.app.learn`, to stay clear of the other features. The only shared files touched are `AstroLingoApp.kt` (Learn tab and delete-everything) and `strings.xml`.
- **Progress and bookmarks** are not birth data. They are kept in app-private storage, which is already excluded from backup, and are wiped by "Delete my details".
- No new Google Maven dependency. `kotlinx-serialization-json` (Maven Central) parses the content.

### Stories, one pull request each (stacked)
1. Learn design, ADR 0011 and the learn engine (model, loader, path, knowledge check, progress, bookmarks) with tests.
2. The Guru's course: all 14 chapters, with content tests.
3. Learn home and the card player: the path, chapter sheet, swipe-up cards, graha flip cards, your-chart cards, quizzes, lesson done, motion, progress saved. (Home and player were joined: one without the other can't be used.)
4. Bookmarks and the Saved list.
5. Knowledge check that skips chapters.

### QA approach
- Content tests on every file: schema, unique ids, 3 quiz cards per lesson, the answer index inside the options, length limits, 2 check questions per chapter, every `hi` either empty or filled.
- Engine tests: path states, next lesson, the check marking chapters known, progress encoding round-trip, bookmark add/remove/order.
- Compose screenshot tests of Learn home, a learn card, a quiz card, lesson done, Saved and the check, rendered in CI.
- Manual checks: TalkBack, 200% font, animations off, dark theme.

### Open questions for the PO (defaults already built)
1. **Lock chapters until the one before is done?** Default: no, the path recommends an order but every chapter is open.
2. **Two questions per chapter in the check?** Default: yes, both must be right to skip.
3. **XP and streaks on lessons?** Default: not in this module; the streak story in the sprint plan picks up "lesson completed" later.

## 3. Richer graha cards (PO, 20 Oct 2026)
The PO found the graha cards too thin to learn from. After a review (plan and wireframe in the project files,
`learn/planet-cards/`), the PO approved:
- **One lesson per graha** in "The nine grahas", so a learner can open any graha directly: Meet the grahas, then Surya,
  Chandra, Mangal, Budha, Guru, Shukra, Shani, Rahu, Ketu, and **Navagraha Sangraha** (sangraha: a gathering) last.
- **The same cards for every graha:** Meet (flip card), Nature and signs (a coloured benefic/malefic/neutral/shadow
  badge and rows: owns, exalted (uchcha), debilitated (neecha), moolatrikona, day), What it rules (chip groups: people,
  self, body (traditional), work; then strong and troubled), At a glance (friends, aspects, dasha years, nakshatras,
  speed, colour and direction), and Your graha from the learner's own chart. Then three quizzes.
- **Navagraha Sangraha:** all nine on one page (think of, nature, signs, day, colour). A tap opens it full screen with a
  faint AstroLingo watermark behind.
- **Graha tags** on cards, so the chart bubble marks the graha a card is about even when its picture is a zodiac or
  house diagram. The content checker fails any card whose title names a graha without the tag.
- Saved progress follows the moved lessons and cards (`ContentMoves`): a learner who finished the old lessons keeps
  the chapter finished, and saved cards point at their new versions.
