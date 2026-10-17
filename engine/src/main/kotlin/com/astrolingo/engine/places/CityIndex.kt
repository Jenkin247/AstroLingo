// Offline city search for birthplaces and the current place. Bundled GeoNames data; nothing leaves the phone.
package com.astrolingo.engine.places

import java.io.InputStream
import java.text.Normalizer
import java.time.ZoneId

/**
 * A place someone can pick.
 *
 * @property id GeoNames id, stable across data updates; store this, not the name.
 * @property name the place's name.
 * @property admin state or province, blank when unknown.
 * @property countryCode ISO 3166 two-letter code.
 * @property latitudeDegrees north positive.
 * @property longitudeDegrees east positive.
 * @property zone IANA time zone.
 * @property population for ranking search results.
 * @property aliases older or common names, such as Bombay for Mumbai.
 */
data class City(
    val id: Long,
    val name: String,
    val admin: String,
    val countryCode: String,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val zone: ZoneId,
    val population: Long,
    val aliases: List<String>,
) {
    /** What the picker shows, for example "Pune, Maharashtra, IN". */
    val label: String get() = listOf(name, admin, countryCode).filter { it.isNotBlank() }.joinToString(", ")
}

/** The bundled city list, searchable by name or alias. Load once; searching is then in memory. */
class CityIndex private constructor(private val cities: List<City>) {
    private val searchNames: List<List<String>> = cities.map { city -> (listOf(city.name) + city.aliases).map(::fold) }
    private val byId: Map<Long, City> = cities.associateBy { it.id }

    /** Number of places. */
    val size: Int get() = cities.size

    /** The place with a stored id, or null if a data update removed it. */
    fun byId(id: Long): City? = byId[id]

    /**
     * Places matching what the user typed, best first.
     *
     * Ranking: exact name or alias, then name or alias starting with the query, then any word starting with it;
     * bigger places first within each, so "banga" offers Bengaluru (via Bangalore) before small towns. Accents and case are ignored, so "pune" finds Pune and "bombay" finds Mumbai.
     *
     * @param query what the user typed.
     * @param limit most results to return.
     */
    fun search(query: String, limit: Int = DEFAULT_LIMIT): List<City> {
        val q = fold(query).trim()
        if (q.length < MIN_QUERY) return emptyList()
        return cities.indices
            .mapNotNull { i -> rank(searchNames[i], q)?.let { i to it } }
            .sortedWith(compareBy<Pair<Int, Int>> { it.second }.thenByDescending { cities[it.first].population })
            .take(limit)
            .map { cities[it.first] }
    }

    private fun rank(names: List<String>, q: String): Int? {
        return when {
            names.any { it == q } -> RANK_EXACT
            names.any { it.startsWith(q) } -> RANK_PREFIX
            names.any { n -> n.split(' ', '-').any { it.startsWith(q) } } -> RANK_WORD
            else -> null
        }
    }

    companion object {
        private const val DEFAULT_LIMIT = 10
        private const val MIN_QUERY = 2
        private const val RANK_EXACT = 0
        private const val RANK_PREFIX = 1
        private const val RANK_WORD = 2
        private const val COLUMNS = 9
        private val diacritics = Regex("\\p{Mn}+")

        /** Loads the city list bundled with the engine. */
        fun bundled(): CityIndex = load(checkNotNull(CityIndex::class.java.getResourceAsStream("/places/cities.tsv")))

        /** Loads a city list in the bundled tab-separated format (see tools/cities/build_cities.py). */
        fun load(input: InputStream): CityIndex = input.bufferedReader().useLines { lines ->
            CityIndex(
                lines.filterNot { it.startsWith("#") || it.startsWith("id\t") || it.isBlank() }
                    .map(::parse)
                    .toList(),
            )
        }

        private fun parse(line: String): City {
            val c = line.split('\t')
            require(c.size == COLUMNS) { "expected $COLUMNS columns: $line" }
            return City(
                id = c[0].toLong(),
                name = c[1],
                admin = c[2],
                countryCode = c[3],
                latitudeDegrees = c[4].toDouble(),
                longitudeDegrees = c[5].toDouble(),
                zone = ZoneId.of(c[6]),
                population = c[7].toLong(),
                aliases = c[8].split('|').filter { it.isNotBlank() },
            )
        }

        /** Lower case without accents: "Mahesāna" becomes "mahesana". */
        internal fun fold(text: String): String =
            diacritics.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase()
    }
}
