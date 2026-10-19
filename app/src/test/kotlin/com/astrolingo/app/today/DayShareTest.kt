package com.astrolingo.app.today

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// Fictional people only.
class DayShareTest {
    private val share = DayShare(
        title = "Asha's day",
        date = "Thursday, October 22, 2026",
        strength = "A strong day",
        headline = "Start the thing you've been putting off",
        tips = listOf("Make the call", "Say yes to plans"),
        colour = "Colour of the day: Yellow",
        rahu = "Rahu kaal in Pune: 1:30 PM to 3:00 PM",
        footer = "Shared from AstroLingo",
    )

    @Test fun `the text reads top to bottom in short lines`() {
        assertThat(share.text()).isEqualTo(
            """
            Asha's day · Thursday, October 22, 2026
            A strong day: Start the thing you've been putting off
            ✓ Make the call
            ✓ Say yes to plans
            Colour of the day: Yellow
            Rahu kaal in Pune: 1:30 PM to 3:00 PM

            Shared from AstroLingo
            """.trimIndent(),
        )
    }

    @Test fun `a day with no sunrise shares the colour only`() {
        val polar = share.copy(strength = null, headline = null, tips = emptyList(), rahu = null)
        assertThat(polar.text()).isEqualTo("Asha's day · Thursday, October 22, 2026\nColour of the day: Yellow\n\nShared from AstroLingo")
    }

    @Test fun `printing it names nobody`() {
        assertThat(share.toString()).doesNotContain("Asha")
    }
}
