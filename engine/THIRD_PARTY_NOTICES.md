# Third-party code in the engine

## Astronomy Engine (Kotlin)
- Source: https://github.com/cosinekitty/astronomy, file `source/kotlin/src/main/kotlin/io/github/cosinekitty/astronomy/astronomy.kt`, master branch, copied 2026-10-16, unmodified.
- Location here: `src/main/kotlin/io/github/cosinekitty/astronomy/astronomy.kt`
- Licence: MIT, Copyright (c) 2019-2025 Don Cross. The full licence text is kept at the top of the file.
- Why vendored: it is a single file with no dependencies, and copying it pins the exact version we tested.
- Accuracy stated by the author: about ±1 arcminute for planet positions, enough for a kundli.

## GeoNames city data

`src/main/resources/places/cities.tsv` is built from GeoNames data (https://www.geonames.org), licensed under
Creative Commons Attribution 4.0 (https://creativecommons.org/licenses/by/4.0/). It was taken from the
geonamescache 3.0.2 and reverse_geocoder 1.5.1 packages and changed: filtered by population, state names joined
by location, and a short list of older Indian city names added as aliases. The app credits GeoNames in Me > About.
