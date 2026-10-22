// The pass mark that opens the next lesson: two thirds of a lesson's quiz right (2 of 3), retries without limit.
package com.astrolingo.learn.path

object QuizPass {
    /** How many of [total] quiz questions must be right: two thirds, rounded up. A lesson without a quiz passes. */
    fun needed(total: Int): Int = (total * 2 + 2) / 3

    fun passed(right: Int, total: Int): Boolean = right >= needed(total)
}
