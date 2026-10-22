// Turns learning progress into key/value text for the app's private store, and back. Unit tested on the JVM.
package com.astrolingo.learn.progress

import com.astrolingo.learn.review.Leitner
import com.astrolingo.learn.review.ReviewItem
import com.astrolingo.learn.stars.Stars

/**
 * Key/value layout of saved progress. A version key lets later releases migrate old values.
 * Version 2 added study time (LS-4) and Review (LS-5); version 3 added Stars. Older values are read as they are,
 * without what came later.
 */
object LearnProgressCodec {
    const val VERSION = 3

    private const val K_VERSION = "version"
    private const val K_COMPLETED = "completed_lessons"
    private const val K_KNOWN = "known_chapters"
    private const val K_CHECK = "check_answered"
    private const val K_BOOKMARKS = "bookmarks"
    private const val K_RESUME = "resume"
    private const val K_STUDY = "study_seconds"
    private const val K_REVIEW = "review"
    private const val K_REVIEW_TODAY = "review_today"
    private const val K_STARS = "stars"

    // Content ids are lowercase letters, digits, dots and hyphens, so these separators never clash.
    private const val LINE = "\n"
    private const val FIELD = "\t"
    private const val NONE = "-"

    fun encode(progress: LearnProgress): Map<String, String> = buildMap {
        put(K_VERSION, VERSION.toString())
        put(K_COMPLETED, progress.completedLessons.sorted().joinToString(LINE))
        put(K_KNOWN, progress.knownChapters.sorted().joinToString(LINE))
        put(K_CHECK, progress.checkAnswered.toString())
        put(K_BOOKMARKS, progress.bookmarks.joinToString(LINE) { "${it.cardId}$FIELD${it.savedAtMillis}" })
        progress.resume?.let { put(K_RESUME, "${it.lessonId}$FIELD${it.cardIndex}") }
        put(K_STUDY, progress.studySeconds.toSortedMap().entries.joinToString(LINE) { "${it.key}$FIELD${it.value}" })
        put(K_REVIEW, progress.review.values.sortedBy { it.cardId }.joinToString(LINE) { "${it.cardId}$FIELD${it.box}$FIELD${it.dueDay}" })
        progress.reviewDay?.let { put(K_REVIEW_TODAY, "$it$FIELD${progress.reviewCount}") }
        with(progress.stars) {
            put(K_STARS, listOf(total, day, earnedToday, visitDay, reviewDay).joinToString(FIELD) { it?.toString() ?: NONE })
        }
    }

    /**
     * Rebuilds progress, or returns empty progress when nothing is saved or the values can't be read. Lessons and
     * cards that moved in a content update are followed to their new ids ([ContentMoves]).
     */
    fun decode(values: Map<String, String>): LearnProgress = runCatching {
        if (values[K_VERSION]?.toInt() !in 1..VERSION) return LearnProgress()
        val saved = LearnProgress(
            completedLessons = values[K_COMPLETED].lines().toSet(),
            knownChapters = values[K_KNOWN].lines().toSet(),
            checkAnswered = values[K_CHECK]?.toBooleanStrict() ?: false,
            bookmarks = values[K_BOOKMARKS].lines().map { line ->
                val (id, millis) = line.split(FIELD)
                Bookmark(id, millis.toLong())
            },
            resume = values[K_RESUME]?.split(FIELD)?.let { (lesson, index) -> ResumePoint(lesson, index.toInt()) },
            studySeconds = values[K_STUDY].lines().associate { line ->
                val (day, seconds) = line.split(FIELD)
                day.toLong() to seconds.toInt()
            },
            review = values[K_REVIEW].lines().associate { line ->
                val (id, box, due) = line.split(FIELD)
                id to ReviewItem(id, box.toInt().coerceIn(1, Leitner.BOXES), due.toLong())
            },
            reviewDay = values[K_REVIEW_TODAY]?.split(FIELD)?.first()?.toLong(),
            reviewCount = values[K_REVIEW_TODAY]?.split(FIELD)?.get(1)?.toInt() ?: 0,
            stars = values[K_STARS]?.split(FIELD)?.let { (total, day, earned, visit, review) ->
                Stars(total.toInt(), day.dayOrNull(), earned.toInt(), visit.dayOrNull(), review.dayOrNull())
            } ?: Stars(),
        )
        ContentMoves.apply(saved)
    }.getOrDefault(LearnProgress())

    private fun String.dayOrNull(): Long? = if (this == NONE) null else toLong()

    private fun String?.lines(): List<String> = this?.split(LINE)?.filter { it.isNotEmpty() }.orEmpty()
}
