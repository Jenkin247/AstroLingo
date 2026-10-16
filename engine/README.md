# engine (jyotish)

Pure-Kotlin Vedic astrology engine for AstroLingo. It has no Android imports, so it
can later be shared with iOS through Kotlin Multiplatform (see `docs/adr/0001-module-structure.md`).

## Conventions
- Sidereal zodiac, Lahiri (Chitrapaksha) ayanamsa, whole-sign houses, true node.
- Angles are in degrees, normalised to `[0, 360)`. Times are UTC.
- Every astrology rule carries a comment naming its source from the Astro Guru's spec.

## Public API (so far)
- `KundliCalculator.calculate(BirthMoment)`: the D1 chart with all nine grahas, lagna and whole-sign houses.
- `Ayanamsa.lahiri(julianCenturiesTt)`: Lahiri ayanamsa in degrees.
- `Rashi.of(siderealLongitude)`: the sign (rashi) a sidereal longitude falls in.
- `Nakshatra.positionOf(siderealLongitude)`: the nakshatra and pada.

`BirthMoment` holds only the UTC moment and coordinates: no name and no address.
Converting local birth time to UTC (with historic time zones) happens in the app's data layer.

## Third-party code
Planet positions come from Astronomy Engine (MIT), vendored unmodified. See `THIRD_PARTY_NOTICES.md`.

## Testing
```
./gradlew -p engine test
```
Smoke tests check the pipeline against published positions and the 2026 Makar Sankranti moment.
The Astro Guru's 50-chart golden set becomes the accuracy gate (within 1 arcminute) next.
