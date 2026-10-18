# Learn content format and writing rules

Owner: Astro Guru. Read by: `learn/` (loader and tests), the Android app, translators.
Files: `learn/src/main/resources/content/learn/`. `course.json` lists the chapter files in order; each chapter is one file.

## course.json
```json
{ "schemaVersion": 1, "contentVersion": 1, "chapters": ["01-welcome.json", "02-grahas.json"] }
```

## Chapter file
```json
{
  "schemaVersion": 1,
  "contentVersion": 1,
  "id": "grahas",
  "glyph": "☉",
  "title": { "en": "The nine grahas", "hi": "" },
  "summary": { "en": "Meet the nine grahas, the actors of every kundli.", "hi": "" },
  "outcome": { "en": "name the nine grahas, what each signifies and its nature", "hi": "" },
  "check": [ QUIZ, QUIZ ],
  "lessons": [
    {
      "id": "grahas.sun-moon",
      "title": { "en": "Sun and Moon", "hi": "" },
      "minutes": 4,
      "cards": [ CARD, CARD, … ]
    }
  ]
}
```

`outcome` (required, up to 70 characters) is what the learner can do after the chapter. It is written to follow
"You'll be able to" on the chapter tile and "You can now" when the chapter is finished, so it starts in lowercase
with a verb. The Guru writes it from the chapter's learning goal (LS-2).

### Cards
Every card has a unique `id`: the lesson id, a dot and a number (`grahas.sun-moon.1`). Check questions use the chapter id, `.check.` and a number (`grahas.check.1`). Ids never change once released, because bookmarks and progress refer to them.

**learn**
```json
{
  "id": "grahas.sun-moon.1",
  "type": "learn",
  "title": { "en": "Surya, the Sun" },
  "body": { "en": "Surya is the soul of the chart: confidence, father, authority and vitality." },
  "term": { "devanagari": "सूर्य", "roman": "Surya" },
  "visual": { "kind": "glyph", "value": "☉" }
}
```
`term` and `visual` are optional. Visual kinds:
| kind | value | Shows |
|---|---|---|
| `glyph` | one or two characters | A large symbol (☉ ☽ ♂ ☿ ♃ ♀ ♄ ☊ ☋, sign glyphs ♈…♓, or ★ ✦ ॐ) |
| `zodiac` | rashi number 1–12, or 0 for none | The zodiac wheel with that sign lit |
| `houses` | house number 1–12, or 0 for none | The North Indian house diagram with that house lit |
| `nakshatras` | nakshatra number 1–27, or 0 for none | The ring of 27 nakshatras with that one lit |
| `graha` | `SURYA`, `CHANDRA`, `MANGAL`, `BUDHA`, `GURU`, `SHUKRA`, `SHANI`, `RAHU` or `KETU` | A drawn picture of that graha (Shani with its rings, Surya glowing…) |

**Flip cards** (PO, 17 Oct 2026): add `"flip": true` to a learn card with a visual and its front shows only the
picture and the title; the learner taps to turn it over and read the text, then swipes up. Used for the card that
introduces each graha.

**yourChart**: the lesson applied to the user's own kundli.
```json
{
  "id": "grahas.sun-moon.4",
  "type": "yourChart",
  "fact": "MOON_RASHI",
  "title": { "en": "Your Moon" },
  "body": { "en": "Your Moon is in {value}. This is your Chandra rashi, the sign your daily card is read from." },
  "unknownTime": { "en": "" }
}
```
`{value}` is replaced by the app. Facts and what `{value}` becomes:
| fact | {value} example | Needs birth time |
|---|---|---|
| `SUN_RASHI` | Simha (Leo) | no |
| `MOON_RASHI` | Vrishabha (Taurus) | no |
| `MOON_NAKSHATRA` | Rohini, pada 2 | no |
| `MOON_NAKSHATRA_LORD` | Chandra (Moon) | no |
| `LAGNA` | Kanya (Virgo) | yes |
| `LAGNA_LORD` | Budha (Mercury) | yes |
| `SUN_HOUSE`, `MOON_HOUSE`, `GURU_HOUSE`, `SHANI_HOUSE` | 10 | yes |

For a fact that needs the birth time, `unknownTime` is required: what to say when the user doesn't know their time (no `{value}`).

**quiz**
```json
{
  "id": "grahas.sun-moon.7",
  "type": "quiz",
  "question": { "en": "Which graha stands for the mind?" },
  "options": [ { "en": "Surya" }, { "en": "Chandra" }, { "en": "Shani" } ],
  "answer": 1,
  "explanation": { "en": "Chandra, the Moon, is the mind (manas): feelings, comfort and mother." }
}
```
`answer` is the 0-based index of the right option.

## Rules the tests enforce
- `schemaVersion` 1. Ids unique across the whole course; card ids start with their lesson id, check ids with `<chapter>.check.`.
- Each chapter: 3 to 5 lessons and exactly 2 `check` questions (quiz cards).
- Each lesson: 5 to 7 learn or yourChart cards, then exactly 3 quiz cards, last. `minutes` 3 to 5.
- Lengths (English): title ≤ 40, body ≤ 320, question ≤ 140, option ≤ 60, explanation ≤ 260 characters.
- Quiz: 3 or 4 options, no duplicates, `answer` inside the options.
- Every text has a non-empty `en`. `hi` may be missing or empty (falls back to English) until Hindi is written.
- `yourChart` bodies contain `{value}`; facts that need the time have a non-empty `unknownTime`.

## Writing rules (Astro Guru)
- Lahiri ayanamsa, whole-sign houses, true node, Parashari tradition. Name a source in the text only when it helps the learner ("Parashara says…").
- Sanskrit term first with the English gloss on first use: graha (planet), rashi (sign), bhava (house).
- Plain, warm, short sentences in AstroLingo's voice. One idea per card.
- Astrology is cultural and spiritual guidance, never certainty: "tends to", "traditionally".
- Never predict death, illness, accidents, divorce or infertility. No fear: doshas and sade sati are explained in balance, with cancellations. No paid remedies, gemstones or pujas.
- Non-sectarian, no caste or gender bias. Examples use fictional people only.
- Inspired by the classic books' order and ideas; never copy or closely paraphrase their text or any modern book.
