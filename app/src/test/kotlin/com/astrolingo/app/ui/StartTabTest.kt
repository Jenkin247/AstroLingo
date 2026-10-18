package com.astrolingo.app.ui

import com.astrolingo.app.onboarding.Purpose
import org.junit.Assert.assertEquals
import org.junit.Test

class StartTabTest {
    @Test
    fun learnersOpenOnLearn() {
        assertEquals(Tab.LEARN, startTab(Purpose.LEARN))
    }

    @Test
    fun guidanceUsersOpenOnToday() {
        assertEquals(Tab.TODAY, startTab(Purpose.GUIDANCE))
    }
}
