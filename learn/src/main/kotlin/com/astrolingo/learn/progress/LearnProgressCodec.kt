// Turns learning progress into key/value text for the app's private store, and back. Unit tested on the JVM.
package com.astrolingo.learn.progress

/** Key/value layout of saved progress. A version key lets later releases migrate old values. */
object LearnProgressCodec {
    const val VERSION = 1

    private const val K_VERSION = "version"
    private const val K_COMPLETED = "completed_lessons"
    private const val K_KNOWN = "known_chapters"
    private const val K_CHECK = "check_answered"
    private const val K_BOOKMARKS = "bookmarks"
    private const val K_RESUME = "resume"

    // Content ids are lowercase letters, digits, dots and hyphens, so these separators never clash.
    private const val LINE = "\n"
    private const val FIELD = "\t"

    fun encode(progress: LearnProgress): Map<String, String> = buildMap {
        put(K_VERSION, VERSION.toString())
        put(K_COMPLETED, progress.completedLessons.sorted().joinToString(LINE))
        put(K_KNOWN, progress.knownChapters.sorted().joinToString(LINE))
        put(K_CHECK, progress.checkAnswered.toString())
        put(K_BOOKMARKS, progress.bookmarks.joinToString(LINE) { "${it.cardId}$FIELD${it.savedAtMillis}" })
        progress.resume?.let { put(K_RESUME, "${it.lessonId}$FIELD${it.cardIndex}") }
    }

    /** Rebuilds progress, or returns empty progress when nothing is saved or the values can't be read. */
    fun decode(values: Map<String, String>): LearnProgress = runCatching {
        if (values[K_VERSION]?.toInt() != VERSION) return LearnProgress()
        LearnProgress(
            completedLessons = values[K_COMPLETED].lines().toSet(),
            knownChapters = values[K_KNOWN].lines().toSet(),
            checkAnswered = values[K_CHECK]?.toBooleanStrict() ?: false,
            bookmarks = values[K_BOOKMARKS].lines().map { line ->
                val (id, millis) = line.split(FIELD)
                Bookmark(id, millis.toLong())
            },
            resume = values[K_RESUME]?.split(FIELD)?.let { (lesson, index) -> ResumePoint(lesson, index.toInt()) },
        )
    }.getOrDefault(LearnProgress())

    private fun String?.lines(): List<String> = this?.split(LINE)?.filter { it.isNotEmpty() }.orEmpty()
}
