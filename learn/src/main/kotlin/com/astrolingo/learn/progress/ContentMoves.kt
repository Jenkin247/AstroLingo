// Where lessons and cards went when the content was reorganised, so saved progress follows them. Content version 2
// (PO, 20 Oct 2026) split the five Grahas lessons into one per graha.
package com.astrolingo.learn.progress

object ContentMoves {
    /** Old lesson id to the lessons that now teach the same thing. */
    val LESSONS: Map<String, List<String>> = mapOf(
        "grahas.sun-moon" to listOf("grahas.meet", "grahas.surya", "grahas.chandra"),
        "grahas.mars-mercury-jupiter" to listOf("grahas.mangal", "grahas.budha", "grahas.guru"),
        "grahas.venus-saturn" to listOf("grahas.shukra", "grahas.shani"),
        "grahas.rahu-ketu" to listOf("grahas.rahu", "grahas.ketu"),
        "grahas.nature" to listOf("grahas.sangraha"),
    )

    /** Old card id to the card that now says the same thing; cards not listed have no successor. */
    val CARDS: Map<String, String> = mapOf(
        "grahas.sun-moon.1" to "grahas.meet.1",
        "grahas.sun-moon.2" to "grahas.surya.1",
        "grahas.sun-moon.3" to "grahas.surya.2",
        "grahas.sun-moon.4" to "grahas.chandra.1",
        "grahas.sun-moon.5" to "grahas.chandra.2",
        "grahas.mars-mercury-jupiter.1" to "grahas.mangal.1",
        "grahas.mars-mercury-jupiter.2" to "grahas.mangal.2",
        "grahas.mars-mercury-jupiter.3" to "grahas.budha.1",
        "grahas.mars-mercury-jupiter.4" to "grahas.budha.2",
        "grahas.mars-mercury-jupiter.5" to "grahas.guru.1",
        "grahas.mars-mercury-jupiter.7" to "grahas.guru.6",
        "grahas.mars-mercury-jupiter.9" to "grahas.budha.6",
        "grahas.venus-saturn.1" to "grahas.shukra.1",
        "grahas.venus-saturn.2" to "grahas.shukra.2",
        "grahas.venus-saturn.3" to "grahas.shani.1",
        "grahas.venus-saturn.4" to "grahas.shani.2",
        "grahas.venus-saturn.7" to "grahas.shani.6",
        "grahas.venus-saturn.8" to "grahas.shukra.6",
        "grahas.rahu-ketu.4" to "grahas.rahu.1",
        "grahas.rahu-ketu.5" to "grahas.ketu.1",
        "grahas.rahu-ketu.7" to "grahas.rahu.6",
        "grahas.rahu-ketu.9" to "grahas.ketu.6",
        "grahas.nature.4" to "grahas.sangraha.3",
        "grahas.nature.5" to "grahas.sangraha.6",
        "grahas.nature.8" to "grahas.sangraha.8",
        "grahas.nature.9" to "grahas.sangraha.9",
    )

    /**
     * Progress with old ids moved to their successors: a finished old lesson finishes its new lessons, saved and
     * Review cards follow their card (keeping the earlier save when two land on one card), and a resume point in an
     * old lesson is dropped. Ids that didn't move are left as they are.
     */
    fun apply(progress: LearnProgress): LearnProgress {
        if (progress.completedLessons.none { it in LESSONS } && progress.resume?.lessonId !in LESSONS &&
            progress.bookmarks.none { it.cardId in CARDS } && progress.review.keys.none { it in CARDS }
        ) {
            return progress
        }
        fun card(id: String) = CARDS[id] ?: id
        return progress.copy(
            completedLessons = progress.completedLessons.flatMap { LESSONS[it] ?: listOf(it) }.toSet(),
            bookmarks = progress.bookmarks.map { it.copy(cardId = card(it.cardId)) }.distinctBy { it.cardId },
            resume = progress.resume?.takeIf { it.lessonId !in LESSONS },
            review = progress.review.values.map { it.copy(cardId = card(it.cardId)) }
                .groupBy { it.cardId }
                .mapValues { (_, items) -> items.minBy { it.box } },
        )
    }
}
