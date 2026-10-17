# engine (jyotish)

Pure-Kotlin Vedic astrology engine for AstroLingo. It has no Android imports, so it
can later be shared with iOS through Kotlin Multiplatform (see `docs/adr/0001-module-structure.md`).

## Conventions
- Sidereal zodiac, Lahiri (Chitrapaksha) ayanamsa, whole-sign houses, true node.
- Angles are in degrees, normalised to `[0, 360)`. Times are UTC.
- Every astrology rule carries a comment naming its source from the Astro Guru's spec.

## Public API (so far)
- `Rashi.of(siderealLongitude)`: the sign (rashi) a sidereal longitude falls in.
- `Nakshatra.positionOf(siderealLongitude)`: the nakshatra and pada.

## Testing
```
./gradlew -p engine test
```
Golden-value tests against the Guru's 50 reference charts arrive with the planet-position code.
