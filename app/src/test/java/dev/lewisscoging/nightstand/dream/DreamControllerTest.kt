package dev.lewisscoging.nightstand.dream

import dev.lewisscoging.nightstand.core.NowProvider
import dev.lewisscoging.nightstand.settings.HourFormat
import dev.lewisscoging.nightstand.settings.NightstandSettings
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DreamControllerTest {
    private class Fixture(
        testScope: TestScope,
        initialSettings: NightstandSettings,
        systemIs24Hour: Boolean,
        initialOrientation: Boolean
    ) {
        val orientation = MutableStateFlow(initialOrientation)
        var orientationSubscriptions = 0
        val settings = MutableStateFlow(initialSettings)
        val visibilityChanges = mutableListOf<Pair<Long, Boolean>>()
        var nowCalls = 0
        val controller = DreamController(
            scope = testScope.backgroundScope,
            settings = settings,
            nowProvider = NowProvider {
                nowCalls++
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(testScope.testScheduler.currentTime),
                    ZoneOffset.UTC
                )
            },
            systemIs24Hour = { systemIs24Hour },
            datePattern = { TEST_DATE_PATTERN },
            orientation = orientation.onStart { orientationSubscriptions++ }
        )

        init {
            testScope.backgroundScope.launch {
                controller.uiState.collect { state ->
                    val lastVisible = visibilityChanges.lastOrNull()?.second
                    if (lastVisible != state.clockVisible) {
                        visibilityChanges +=
                            testScope.testScheduler.currentTime to state.clockVisible
                    }
                }
            }
            testScope.runCurrent()
        }
    }

    private fun TestScope.fixture(
        settings: NightstandSettings = NightstandSettings(),
        systemIs24Hour: Boolean = true,
        orientationOk: Boolean = true
    ) = Fixture(this, settings, systemIs24Hour, orientationOk)

    private fun timeTextAt(hourFormat: HourFormat, systemIs24Hour: Boolean): String {
        var timeText = ""
        runTest {
            val fixture = fixture(
                NightstandSettings(
                    use24Hour = hourFormat,
                    startTime = ALWAYS_ON_MINUTES,
                    endTime = ALWAYS_ON_MINUTES
                ),
                systemIs24Hour = systemIs24Hour
            )
            advanceTimeBy(THIRTEEN_OH_FIVE_MILLIS)
            fixture.controller.onTap()
            timeText = fixture.controller.uiState.value.timeText
        }
        return timeText
    }

    @Test
    fun tapShowsTheClockWithTheCurrentTime() = runTest {
        val fixture = fixture()
        advanceTimeBy(3_600_000)
        fixture.controller.onTap()
        assertTrue(fixture.controller.uiState.value.clockVisible)
        assertEquals("01:00", fixture.controller.uiState.value.timeText)
    }

    @Test
    fun clockHidesExactlyAtTheTimeout() = runTest {
        val fixture = fixture()
        fixture.controller.onTap()
        advanceTimeBy(14_999)
        runCurrent()
        assertTrue(fixture.controller.uiState.value.clockVisible)
        advanceTimeBy(1)
        runCurrent()
        assertFalse(fixture.controller.uiState.value.clockVisible)
    }

    @Test
    fun secondTapMovesTheHide() = runTest {
        val fixture = fixture()
        fixture.controller.onTap()
        advanceTimeBy(10_000)
        fixture.controller.onTap()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(listOf(0L to false, 0L to true, 25_000L to false), fixture.visibilityChanges)
    }

    @Test
    fun rapidTapsGiveExactlyOneHide() = runTest {
        val fixture = fixture()
        repeat(20) {
            fixture.controller.onTap()
            advanceTimeBy(100)
        }
        val lastTapTime = 1_900L
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(
            listOf(0L to false, 0L to true, lastTapTime + 15_000 to false),
            fixture.visibilityChanges
        )
    }

    @Test
    fun changedTimeoutAppliesFromTheNextTap() = runTest {
        val fixture = fixture()
        fixture.controller.onTap()
        fixture.settings.value = fixture.settings.value.copy(clockTimeoutSeconds = 5)
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        fixture.controller.onTap()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(
            listOf(0L to false, 0L to true, 15_000L to false, 60_000L to true, 65_000L to false),
            fixture.visibilityChanges
        )
    }

    @Test
    fun hiddenClockReadsNowOnlyForTheMinuteWindowCheck() = runTest {
        val fixture = fixture()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(2, fixture.nowCalls)
        fixture.controller.onTap()
        advanceTimeBy(15_000)
        runCurrent()
        assertFalse(fixture.controller.uiState.value.clockVisible)
        val callsWhenHidden = fixture.nowCalls
        assertTrue(callsWhenHidden > 1)
        advanceTimeBy(3_600_000)
        runCurrent()
        assertEquals(callsWhenHidden + 60, fixture.nowCalls)
    }

    @Test
    fun timeTextRollsOverAtTheMinute() = runTest {
        val fixture = fixture()
        advanceTimeBy(59_500)
        fixture.controller.onTap()
        assertEquals("00:00", fixture.controller.uiState.value.timeText)
        advanceTimeBy(499)
        runCurrent()
        assertEquals("00:00", fixture.controller.uiState.value.timeText)
        advanceTimeBy(1)
        runCurrent()
        assertEquals("00:01", fixture.controller.uiState.value.timeText)
    }

    @Test
    fun tickDelaysArePositiveAndAtMostOneSecond() {
        val startOfSecond = ZonedDateTime.of(2026, 11, 8, 23, 59, 59, 0, ZoneOffset.UTC)
        listOf(0, 1, 1_000_000, 500_000_000, 999_000_000, 999_999_999).forEach { nanos ->
            val delayMillis = millisUntilNextSecond(startOfSecond.withNano(nanos))
            assertTrue("delay $delayMillis at $nanos ns", delayMillis in 1L..1_000L)
        }
        assertEquals(1_000L, millisUntilNextSecond(startOfSecond))
        assertEquals(500L, millisUntilNextSecond(startOfSecond.withNano(500_000_000)))
    }

    @Test
    fun ticksLandOnWholeSeconds() = runTest {
        val fixture = fixture()
        val tickTimes = mutableListOf<Long>()
        backgroundScope.launch {
            fixture.controller.uiState.collect { tickTimes += testScheduler.currentTime }
        }
        advanceTimeBy(59_700)
        fixture.controller.onTap()
        advanceTimeBy(2_500)
        runCurrent()
        assertEquals(listOf(0L, 59_700L, 60_000L), tickTimes.distinct())
    }

    @Test
    fun dateAppearsOnlyWhenShowDateIsOn() = runTest {
        val withDate = fixture(NightstandSettings(showDate = true))
        withDate.controller.onTap()
        assertEquals("1970-01-01", withDate.controller.uiState.value.dateText)
        val withoutDate = fixture(NightstandSettings(showDate = false))
        withoutDate.controller.onTap()
        assertNull(withoutDate.controller.uiState.value.dateText)
    }

    @Test
    fun turningShowDateOffClearsTheDateOnTheNextTick() = runTest {
        val fixture = fixture(NightstandSettings(showDate = true))
        fixture.controller.onTap()
        fixture.settings.value = fixture.settings.value.copy(showDate = false)
        advanceTimeBy(1_000)
        runCurrent()
        assertNull(fixture.controller.uiState.value.dateText)
    }

    @Test
    fun hourFormatFollowsResolveUse24Hour() {
        assertEquals("13:05", timeTextAt(HourFormat.H24, systemIs24Hour = false))
        assertEquals("1:05", timeTextAt(HourFormat.H12, systemIs24Hour = true))
        assertEquals("13:05", timeTextAt(HourFormat.SYSTEM, systemIs24Hour = true))
        assertEquals("1:05", timeTextAt(HourFormat.SYSTEM, systemIs24Hour = false))
    }

    @Test
    fun masterToggleOffAloneDisablesTaps() = runTest {
        val fixture = fixture(NightstandSettings(enabled = false))
        fixture.controller.onTap()
        assertFalse(fixture.controller.tapsAllowed.value)
        assertFalse(fixture.controller.uiState.value.clockVisible)
    }

    @Test
    fun timeWindowAloneDisablesTaps() = runTest {
        val fixture =
            fixture(NightstandSettings(startTime = ONE_AM_MINUTES, endTime = TWO_AM_MINUTES))
        fixture.controller.onTap()
        assertFalse(fixture.controller.tapsAllowed.value)
        assertFalse(fixture.controller.uiState.value.clockVisible)
    }

    @Test
    fun orientationAloneDisablesTaps() = runTest {
        val fixture = fixture(orientationOk = false)
        fixture.controller.onTap()
        assertFalse(fixture.controller.tapsAllowed.value)
        assertFalse(fixture.controller.uiState.value.clockVisible)
    }

    @Test
    fun defaultWindowClosesAtTheSevenOClockMinuteTick() = runTest {
        val fixture = fixture()
        advanceTimeBy(SEVEN_AM_MILLIS - 1)
        runCurrent()
        assertTrue(fixture.controller.tapsAllowed.value)
        advanceTimeBy(1)
        runCurrent()
        assertFalse(fixture.controller.tapsAllowed.value)
        fixture.controller.onTap()
        assertFalse(fixture.controller.uiState.value.clockVisible)
    }

    @Test
    fun orientationIsNeverSubscribedWhenLandscapeIsNotRequired() = runTest {
        val fixture = fixture(NightstandSettings(requireLandscape = false), orientationOk = false)
        advanceTimeBy(60_000)
        runCurrent()
        assertTrue(fixture.controller.tapsAllowed.value)
        assertEquals(0, fixture.orientationSubscriptions)
        fixture.settings.value = fixture.settings.value.copy(requireLandscape = true)
        runCurrent()
        assertEquals(1, fixture.orientationSubscriptions)
        assertFalse(fixture.controller.tapsAllowed.value)
    }

    @Test
    fun gateClosingWhileVisibleHidesTheClockImmediately() = runTest {
        val fixture = fixture()
        fixture.controller.onTap()
        advanceTimeBy(5_000)
        fixture.orientation.value = false
        runCurrent()
        assertFalse(fixture.controller.uiState.value.clockVisible)
        fixture.orientation.value = true
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(listOf(0L to false, 0L to true, 5_000L to false), fixture.visibilityChanges)
    }

    @Test
    fun reopeningAGateReallowsTaps() = runTest {
        val fixture = fixture(NightstandSettings(enabled = false))
        fixture.controller.onTap()
        assertFalse(fixture.controller.uiState.value.clockVisible)
        fixture.settings.value = fixture.settings.value.copy(enabled = true)
        runCurrent()
        assertTrue(fixture.controller.tapsAllowed.value)
        fixture.controller.onTap()
        assertTrue(fixture.controller.uiState.value.clockVisible)
    }

    private companion object {
        const val TEST_DATE_PATTERN = "yyyy-MM-dd"
        const val ALWAYS_ON_MINUTES = 0
        const val ONE_AM_MINUTES = 60
        const val TWO_AM_MINUTES = 120
        const val SEVEN_AM_MILLIS = 7 * 3_600_000L
        const val THIRTEEN_OH_FIVE_MILLIS = (13 * 60 + 5) * 60_000L
    }
}
