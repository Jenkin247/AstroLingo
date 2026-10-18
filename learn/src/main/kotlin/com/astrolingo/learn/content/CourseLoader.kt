// Reads the course from the JSON files bundled with this module (learn/src/main/resources/content/learn/).
package com.astrolingo.learn.content

import java.io.InputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Loads the Learn course. Parsing takes a few tens of milliseconds: call it off the main thread, once. */
object CourseLoader {
    /** The only content schema this app version understands. */
    const val SCHEMA_VERSION = 1

    private const val ROOT = "/content/learn/"

    internal val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        // Visual values are numbers for wheels and strings for glyphs; both are read as text.
        isLenient = true
    }

    @Serializable
    private data class CourseIndex(val schemaVersion: Int, val contentVersion: Int, val chapters: List<String>)

    /** The course bundled with the app. */
    fun bundled(): Course = load { name -> CourseLoader::class.java.getResourceAsStream(ROOT + name) }

    /**
     * Loads a course from files named as in course.json.
     *
     * @param open opens a file by name, or returns null when it is missing.
     * @throws IllegalStateException when a file is missing or has a schema this version doesn't know.
     */
    fun load(open: (String) -> InputStream?): Course {
        val index = json.decodeFromString<CourseIndex>(read(open, "course.json"))
        check(index.schemaVersion == SCHEMA_VERSION) { "course.json has schema ${index.schemaVersion}" }
        val chapters = index.chapters.map { name ->
            json.decodeFromString<Chapter>(read(open, name)).also {
                check(it.schemaVersion == SCHEMA_VERSION) { "$name has schema ${it.schemaVersion}" }
            }
        }
        return Course(index.contentVersion, chapters)
    }

    private fun read(open: (String) -> InputStream?, name: String): String =
        checkNotNull(open(name)) { "Missing content file $name" }.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
