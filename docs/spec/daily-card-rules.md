# Daily card rules

Owner: Astro Guru. Reviewed with the BA and architect, sprint 1. Code: `engine/.../DailyCard.kt` and `Panchang.kt`.

The daily card is chosen by fixed Vedic rules on the phone, never by AI. The rules pick **facts**; the Guru's message library turns facts into words. Same inputs, same card, offline, and no birth data leaves the phone.

## Inputs

| Input | From | Notes |
|---|---|---|
| Birth Moon sign (Chandra rashi) | the user's kundli | Works without a birth time, see below |
| Birth nakshatra (janma nakshatra) | the user's kundli | |
| Today's date | the phone, local time zone | |
| Current place | the phone's chosen city (not GPS by default) | Rahu kaal and sunrise follow where the user is now, not where they were born |

## The day

- **Which day.** The card belongs to the local civil date. A panchang day starts at sunrise, so every Moon value is taken **at that date's sunrise** at the current place. A reminder at 6:00 before a 7:13 sunrise still shows that date's card.
- **Sunrise and sunset.** Visible sunrise: the Sun's upper limb on the horizon with standard refraction, as most printed panchangs use. Checked within 2 minutes of the reference.
- **When the Moon changes during the day.** Guru ruling, sprint 3. We keep the sunrise (udaya) values, as a panchang does, and also list when the Moon leaves that nakshatra or sign before sunset, as printed panchangs list a nakshatra's end time. Muhurta texts (Muhurta Chintamani) use the nakshatra in force at the moment of action, so the card shows both halves: "Until 9:47 am … / From 9:47 am …". The **headline** (strength, theme, gochar house) uses whatever holds for most of the daytime, which is the value at the midpoint between sunrise and sunset; the Moon moves under 13°20′ in any daytime, so it changes star (and sign) at most once. Change times are found by halving to under a second and checked within a minute of the reference.
- **Polar day or night.** No sunrise means no panchang; the card shows the colour and deity only.

## The facts

1. **Vara and its lord.** Sunday Surya, Monday Chandra, Tuesday Mangal, Wednesday Budha, Thursday Guru, Friday Shukra, Saturday Shani.
2. **Colour of the day,** from the lord: Surya orange, Chandra white, Mangal red, Budha green, Guru yellow, Shukra pink, Shani dark blue.
3. **Deity of the day,** from the lord: Surya, Shiva, Hanuman, Ganesha, Vishnu, Lakshmi, Shani. The user can turn deities off at set-up or in Me, and then the app never shows one. That choice stays on the phone.
4. **Chandra gochar.** The house of today's Moon sign counted from the birth Moon sign (birth sign is 1).
   - Favourable: 1, 3, 6, 7, 10, 11
   - Mixed: 2, 5, 9
   - Needs care: 4, 8, 12
5. **Tara bala.** Count from the birth nakshatra (which is 1) to today's, then fold into nine: Janma, Sampat, Vipat, Kshema, Pratyak, Sadhana, Naidhana, Mitra, Parama Mitra.
   - Favourable: Sampat, Kshema, Sadhana, Mitra, Parama Mitra
   - Mixed: Janma. Many families treat the birth star's return as a day for care, so its line leans gentle: "go gently and check in with yourself".
   - Needs care: Vipat, Pratyak, Naidhana
6. **Strength of the day.** Gochar and tara each score +1 (favourable), 0 (mixed) or −1 (needs care).
   - Total 2: **Strong**
   - Total 0 or 1: **Steady**
   - Total below 0: **Gentle**. We never say "bad day"; a gentle day is a day to go easy and finish things rather than start them.
7. **Rahu kaal.** Sunrise to sunset split into eight equal parts. Rahu kaal is part 8 on Sunday, 2 Monday, 7 Tuesday, 5 Wednesday, 6 Thursday, 4 Friday, 3 Saturday.
8. **Tithi.** The Moon's lead over the Sun at sunrise, in 12° steps: 1 to 15 Shukla paksha, 16 to 30 Krishna paksha. 15 is Purnima, 30 Amavasya. Festivals come from a dated list in the content files (sprint 3), not from rules.

## From facts to words

The library is keyed so the Guru writes about 150 short messages, not thousands. The card is laid out to scan at a glance (UX/UI with the Guru, October 2026): no paragraphs, one short headline, a list, and labelled rows.

| Slot | Keyed by | On the card | Messages |
|---|---|---|---|
| Focus | gochar house (12) × strength (3) | headline under the strength pill, under 40 characters | 36 |
| Tips | gochar house (12) × strength (3) | two short tips as a ticked list | 36 × 2 = 72 |
| Star row | tara (9) | bold label "Until 3:38 PM · Star of plenty (Sampat)" (or "Today's star · …"), short tip under it | 9 names + 9 tips |
| Day row | vara (7) | bold label "Monday · Chandra's day", short tip under it | 7 titles + 7 tips |
| Rahu kaal tip | one per strength | note under Rahu kaal in Timings | 3 |

Tips are short phrases without a full stop. Every message is positive and actionable, in AstroLingo's voice, and is reviewed by the Guru before it ships. Hindi has the same arrays in the same order.

## Unknown birth time

Only the birth Moon is needed. With no birth time we use 12:00 local time. If the Moon changes sign or nakshatra during the birth day, Me shows "your birth star may be X or Y" with a picker, and the card uses the user's choice.

## Not in MVP1

- **Dasha lord colour.** The current mahadasha lord can replace the weekday colour when it suits the day better. This waits for the Vimshottari dasha story; the card then shows it as a second swatch.
- Hora, choghadiya, yoga and karana.

## Tests

Engine unit tests cover every row above, plus a star change before midday (Delhi, 17 October 2026, Mula to Purva Ashadha at 9:47 am), a sign change after midday (Delhi, 5 January 2025) and a day with no change: weekday table, tara counting and wrap past Revati, gochar leanings, the strength table, Rahu kaal parts, tithi boundaries, Purnima and Amavasya on real dates, sunrise in Delhi and London, and the polar case.
