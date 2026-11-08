package dev.lewisscoging.nightstand.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTimeoutSliderTest {
    @Test
    fun sliderStepsLandOnEveryFiveSeconds() {
        assertEquals(22, CLOCK_TIMEOUT_SLIDER_STEPS)
        val stops = CLOCK_TIMEOUT_SLIDER_STEPS + 2
        val span = MAX_CLOCK_TIMEOUT_SECONDS - MIN_CLOCK_TIMEOUT_SECONDS
        assertEquals(span, (stops - 1) * CLOCK_TIMEOUT_STEP_SECONDS)
    }
}
