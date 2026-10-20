package com.astrolingo.widget

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class WidgetOfferRuleTest {
    private val first = LocalDate.of(2026, 10, 20)

    @Test fun `not on the first day or the next`() {
        assertThat(WidgetOfferRule.shouldOffer(first, first, alreadyOffered = false, onHomeScreen = false)).isFalse()
        assertThat(WidgetOfferRule.shouldOffer(first, first.plusDays(1), alreadyOffered = false, onHomeScreen = false)).isFalse()
    }

    @Test fun `two days after the first use, and any day after`() {
        assertThat(WidgetOfferRule.shouldOffer(first, first.plusDays(2), alreadyOffered = false, onHomeScreen = false)).isTrue()
        assertThat(WidgetOfferRule.shouldOffer(first, first.plusDays(30), alreadyOffered = false, onHomeScreen = false)).isTrue()
    }

    @Test fun `only once, and never to someone who already has the widget`() {
        assertThat(WidgetOfferRule.shouldOffer(first, first.plusDays(5), alreadyOffered = true, onHomeScreen = false)).isFalse()
        assertThat(WidgetOfferRule.shouldOffer(first, first.plusDays(5), alreadyOffered = false, onHomeScreen = true)).isFalse()
    }
}
