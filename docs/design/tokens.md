# Design tokens

Owner: UX/UI. Code: `app/src/main/kotlin/com/astrolingo/app/ui/theme/`. Screens use these tokens, never raw hex values or ad hoc dp.

## Colour (palette v3, "morning garden")
| Token | Light | Dark | Use |
|---|---|---|---|
| primary | Lagoon #2C7D74 | #7FD0C3 | Buttons, links, Sitara's cloak |
| secondary | Sea glass #A8DDD3 | #8FCFC4 | Chips, progress |
| tertiary | Rose #F4B6CB | #EDA5BD | Celebration accents |
| background | Mist #F6F9F8 | #11171A | Screens |
| onSurface | Deep slate #253042 | #E7EEF0 | Text |
| Tab: Today | Butter #F7DE8F | #EFD27C | Today tab and card |
| Tab: Learn | Lavender #C3B8F2 | #B3A7EE | Learn tab, Sitara's speech |
| Tab: Kundli | Sky #A9D2F5 | #94C4EF | Kundli tab |
| Tab: Me | Peach #F8C1A6 | #F2B297 | Me tab |

The day's colour swatch on the card shows the real colour (red, yellow and so on), not a pastel.

## Type
System sans-serif. Body text is 17sp with 26sp line height for comfortable reading. Headlines are semibold. Every screen must work at 200% font scale (QA screenshot test).

| Style | Size / line | Use |
|---|---|---|
| displaySmall | 34 / 42 | Card headline, kundli reveal |
| headlineMedium | 26 / 34 | Screen titles |
| headlineSmall | 22 / 30 | Section titles |
| titleLarge / titleMedium | 20 / 28, 16 / 24 | Card titles, list items |
| bodyLarge / bodyMedium / bodySmall | 17 / 26, 15 / 22, 13 / 18 | Lessons, card text, captions |
| labelLarge / labelMedium | 15 / 20, 13 / 18 | Buttons, chips |

## Spacing and shape
- Spacing on a 4dp grid: xs 4, sm 8, md 16, lg 24, xl 32, xxl 48. Touch targets are at least 48dp.
- Corners: extraSmall 8, small 12, medium 20 (cards), large 28 (sheets, daily card), extraLarge 36.

## Sitara, first look
![Sitara](sitara.svg)

A calm star-gazer, smiling toward a star. Faith-neutral and gender-neutral: no religious dress or symbols, and a soft bob. Built as a vector (`res/drawable/sitara.xml`, source `docs/design/sitara.svg`), so it is crisp at any size and tiny in the APK. It is shown on the Learn tab now. Lottie idle and "thinking" loops come with Ask Sitara (ADR 0007), and a professional illustrator is planned for MVP2.
