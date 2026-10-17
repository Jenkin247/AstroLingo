package com.astrolingo.engine.places

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

class CityIndexTest {
    private val index = CityIndex.bundled()

    @Test
    fun `bundled list loads with every Indian place and big cities worldwide`() {
        assertThat(index.size).isGreaterThan(35_000)
    }

    @Test
    fun `typing pune finds Pune in Maharashtra first`() {
        val pune = index.search("pune").first()
        assertThat(pune.label).isEqualTo("Pune, Maharashtra, IN")
        assertThat(pune.zone).isEqualTo(ZoneId.of("Asia/Kolkata"))
    }

    @Test
    fun `old names find the renamed city`() {
        assertThat(index.search("Bombay").first().name).isEqualTo("Mumbai")
        assertThat(index.search("calcutta").first().name).isEqualTo("Kolkata")
        assertThat(index.search("Madras").first().name).isEqualTo("Chennai")
        assertThat(index.search("banga").map { it.name }).contains("Bengaluru")
    }

    @Test
    fun `accents and case are ignored`() {
        assertThat(index.search("MAHESANA").map { it.name }).contains("Mahesāna")
    }

    @Test
    fun `same name in two states shows both with their state`() {
        val labels = index.search("Aurangabad").map { it.label }
        assertThat(labels).containsAtLeast("Aurangabad, Maharashtra, IN", "Aurangābād, Bihar, IN")
    }

    @Test
    fun `bigger city wins a shared name`() {
        assertThat(index.search("Hyderabad").first().label).isEqualTo("Hyderabad, Telangana, IN")
    }

    @Test
    fun `places abroad carry their own zone`() {
        val london = index.search("London").first()
        assertThat(london.countryCode).isEqualTo("GB")
        assertThat(london.zone).isEqualTo(ZoneId.of("Europe/London"))
    }

    @Test
    fun `one letter returns nothing`() {
        assertThat(index.search("p")).isEmpty()
    }

    @Test
    fun `stored id finds the same place again`() {
        val pune = index.search("Pune").first()
        assertThat(index.byId(pune.id)).isEqualTo(pune)
    }

    @Test
    fun `birth in Bombay before 1955 asks about Bombay Time`() {
        val mumbai = index.search("Mumbai").first()
        assertThat(HistoricCityClock.toAskAbout(mumbai, LocalDate.of(1950, 5, 1), index)).isEqualTo(HistoricCityClock.BOMBAY_TIME)
        assertThat(HistoricCityClock.toAskAbout(mumbai, LocalDate.of(1960, 5, 1), index)).isNull()
    }

    @Test
    fun `birth in Calcutta before 1948 asks about Calcutta Time and Pune never does`() {
        val kolkata = index.search("Kolkata").first()
        val pune = index.search("Pune").first()
        assertThat(HistoricCityClock.toAskAbout(kolkata, LocalDate.of(1946, 1, 1), index)).isEqualTo(HistoricCityClock.CALCUTTA_TIME)
        assertThat(HistoricCityClock.toAskAbout(pune, LocalDate.of(1946, 1, 1), index)).isNull()
    }
}
