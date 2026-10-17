package com.astrolingo.app.ui.kundli

import com.astrolingo.engine.jyotish.BirthMoment
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.KundliCalculator

/**
 * A fictional person's chart, shown on the Kundli tab until the user has entered their own
 * and used in previews: Asha Testwal, born 15 August 1990 at 06:30 IST in Pune.
 */
object SampleChart {
    private const val BIRTH_UTC_MILLIS = 650_682_000_000L // 1990-08-15T01:00:00Z
    private const val PUNE_LATITUDE = 18.5196
    private const val PUNE_LONGITUDE = 73.8554

    val kundli: Kundli by lazy {
        KundliCalculator.calculate(BirthMoment(BIRTH_UTC_MILLIS, PUNE_LATITUDE, PUNE_LONGITUDE))
    }
}
