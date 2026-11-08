package dev.lewisscoging.nightstand.dream

import dev.lewisscoging.nightstand.core.NowProvider
import dev.lewisscoging.nightstand.settings.NightstandSettings
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DreamControllerDstTest {
    private class Fixture(
        testScope: TestScope,
        start: Instant,
        zone: ZoneId,
        settings: NightstandSettings
    ) {
        var zone: ZoneId = zone
        val nowCallTimes = mutableListOf<Long>()
        val timeTexts = mutableListOf<String>()
        val controller = DreamController(
            scope = testScope.backgroundScope,
            settings = MutableStateFlow(settings),
            nowProvider = NowProvider {
                val virtualMillis = testScope.testScheduler.currentTime
                nowCallTimes += virtualMillis
                ZonedDateTime.ofInstant(start.plusMillis(virtualMillis), this.zone)
            },
            systemIs24Hour = { true },
            datePattern = { DATE_PATTERN },
            orientation = MutableStateFlow(true)
        )

        init {
            testScope.backgroundScope.launch {
                controller.uiState.collect { state ->
                    if (state.clockVisible && timeTexts.lastOrNull() != state.timeText) {
                        timeTexts += state.timeText
                    }
                }
            }
            testScope.runCurrent()
        }
    }

    private fun TestScope.fixture(
        start: Instant,
        settings: NightstandSettings,
        zone: ZoneId = LONDON
    ) = Fixture(this, start, zone, settings)

    private fun TestScope.advanceMinutes(minutes: Long) {
        advanceTimeBy(minutes * MILLIS_PER_MINUTE)
        runCurrent()
    }

    @Test
    fun windowStaysOpenThroughTheRepeatedFallBackHour() = runTest {
        // 00:30Z on 2026-10-25 is 01:30 BST; the window is 01:00-02:00 local.
        val fixture = fixture(
            Instant.parse("2026-10-25T00:30:00Z"),
            NightstandSettings(startTime = 60, endTime = 120)
        )
        assertTrue(fixture.controller.tapsAllowed.value)
        advanceMinutes(29) // 00:59Z = 01:59 BST
        assertTrue(fixture.controller.tapsAllowed.value)
        advanceMinutes(1) // 01:00Z = 01:00 GMT, the repeated hour
        assertTrue(fixture.controller.tapsAllowed.value)
        advanceMinutes(59) // 01:59Z = 01:59 GMT
        assertTrue(fixture.controller.tapsAllowed.value)
        advanceMinutes(1) // 02:00Z = 02:00 GMT
        assertFalse(fixture.controller.tapsAllowed.value)
    }

    @Test
    fun windowClosesAcrossTheSkippedSpringForwardHour() = runTest {
        // 00:30Z on 2027-03-28 is 00:30 GMT; the window is 00:00-01:30 local.
        val fixture = fixture(
            Instant.parse("2027-03-28T00:30:00Z"),
            NightstandSettings(startTime = 0, endTime = 90)
        )
        assertTrue(fixture.controller.tapsAllowed.value)
        advanceMinutes(29) // 00:59 GMT
        assertTrue(fixture.controller.tapsAllowed.value)
        advanceMinutes(1) // 01:00Z = 02:00 BST, past the window end
        assertFalse(fixture.controller.tapsAllowed.value)
    }

    @Test
    fun windowInsideTheSkippedHourNeverOpens() = runTest {
        // The window 01:00-02:00 local does not exist on 2027-03-28.
        val fixture = fixture(
            Instant.parse("2027-03-28T00:00:00Z"),
            NightstandSettings(startTime = 60, endTime = 120)
        )
        repeat(120) {
            assertFalse(fixture.controller.tapsAllowed.value)
            advanceMinutes(1)
        }
        assertFalse(fixture.controller.tapsAllowed.value)
    }

    @Test
    fun tickerDelaysStayPositiveAndBoundedAcrossBothTransitions() {
        val starts = listOf(
            Instant.parse("2026-10-25T00:58:00Z"),
            Instant.parse("2027-03-28T00:58:00Z")
        )
        for (start in starts) {
            for (step in 0 until TRANSITION_SPAN_MILLIS step SAMPLE_STEP_MILLIS) {
                val now = ZonedDateTime.ofInstant(start.plusMillis(step), LONDON)
                val secondDelay = millisUntilNextSecond(now)
                val minuteDelay = millisUntilNextMinute(now)
                assertTrue("second delay $secondDelay at $now", secondDelay in 1..MILLIS_PER_SECOND)
                assertTrue("minute delay $minuteDelay at $now", minuteDelay in 1..MILLIS_PER_MINUTE)
                val nextSecond = now.toInstant().plusMillis(secondDelay)
                val nextMinute = now.toInstant().plusMillis(minuteDelay)
                assertEquals(0, nextSecond.toEpochMilli() % MILLIS_PER_SECOND)
                assertEquals(0, nextMinute.toEpochMilli() % MILLIS_PER_MINUTE)
            }
        }
    }

    @Test
    fun secondTickerFollowsTheFallBackWithoutStalling() = runTest {
        // 00:59:55.500Z is 01:59:55.500 BST; five seconds later local time repeats 01:00.
        val fixture = fixture(
            Instant.parse("2026-10-25T00:59:55.500Z"),
            NightstandSettings(startTime = 0, endTime = 0, clockTimeoutSeconds = 30)
        )
        fixture.controller.onTap()
        val callsBefore = fixture.nowCallTimes.size
        advanceTimeBy(TICK_SPAN_MILLIS)
        runCurrent()
        assertEquals(listOf("01:59", "01:00"), fixture.timeTexts)
        assertTickCount(fixture.nowCallTimes.drop(callsBefore))
    }

    @Test
    fun secondTickerFollowsTheSpringForwardWithoutStalling() = runTest {
        // 00:59:55.500Z is 00:59:55.500 GMT; five seconds later local time jumps to 02:00.
        val fixture = fixture(
            Instant.parse("2027-03-28T00:59:55.500Z"),
            NightstandSettings(startTime = 0, endTime = 0, clockTimeoutSeconds = 30)
        )
        fixture.controller.onTap()
        val callsBefore = fixture.nowCallTimes.size
        advanceTimeBy(TICK_SPAN_MILLIS)
        runCurrent()
        assertEquals(listOf("00:59", "02:00"), fixture.timeTexts)
        assertTickCount(fixture.nowCallTimes.drop(callsBefore))
    }

    @Test
    fun zoneChangeShowsTheNewLocalTimeOnTheNextTick() = runTest {
        val fixture = fixture(
            Instant.parse("2026-06-01T12:00:00Z"),
            NightstandSettings(startTime = 0, endTime = 0, clockTimeoutSeconds = 30),
            zone = ZoneOffset.UTC
        )
        fixture.controller.onTap()
        assertEquals("12:00", fixture.controller.uiState.value.timeText)
        fixture.zone = KOLKATA
        advanceTimeBy(MILLIS_PER_SECOND)
        runCurrent()
        assertEquals("17:30", fixture.controller.uiState.value.timeText)
    }

    @Test
    fun zoneChangeReEvaluatesTheWindowOnTheNextMinuteTick() = runTest {
        // 12:00:30 UTC is outside 17:00-18:00; in Kolkata it is 17:30:30, inside.
        val fixture = fixture(
            Instant.parse("2026-06-01T12:00:30Z"),
            NightstandSettings(startTime = 17 * 60, endTime = 18 * 60),
            zone = ZoneOffset.UTC
        )
        assertFalse(fixture.controller.tapsAllowed.value)
        fixture.zone = KOLKATA
        advanceTimeBy(30 * MILLIS_PER_SECOND)
        runCurrent()
        assertTrue(fixture.controller.tapsAllowed.value)
    }

    /**
     * Ten seconds of ticking: one call per second plus at most one minute-ticker call, each on a
     * whole second (the fixtures start half a second past one).
     */
    private fun assertTickCount(callTimes: List<Long>) {
        val seconds = (TICK_SPAN_MILLIS / MILLIS_PER_SECOND).toInt()
        assertTrue("now() calls: $callTimes", callTimes.size in seconds..seconds + 1)
        callTimes.forEach { assertEquals(HALF_SECOND_MILLIS, it % MILLIS_PER_SECOND) }
        callTimes.zipWithNext().forEach { (earlier, later) -> assertTrue(later >= earlier) }
    }

    private companion object {
        val LONDON: ZoneId = ZoneId.of("Europe/London")
        val KOLKATA: ZoneId = ZoneId.of("Asia/Kolkata")
        const val DATE_PATTERN = "yyyy-MM-dd"
        const val MILLIS_PER_SECOND = 1_000L
        const val MILLIS_PER_MINUTE = 60_000L
        const val TICK_SPAN_MILLIS = 10_000L
        const val TRANSITION_SPAN_MILLIS = 4L * MILLIS_PER_MINUTE
        const val SAMPLE_STEP_MILLIS = 250L
        const val HALF_SECOND_MILLIS = 500L
    }
}
