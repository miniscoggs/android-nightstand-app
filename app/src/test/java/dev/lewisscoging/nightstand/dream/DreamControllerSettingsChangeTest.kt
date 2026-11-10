package dev.lewisscoging.nightstand.dream

import dev.lewisscoging.nightstand.core.NowProvider
import dev.lewisscoging.nightstand.settings.HourFormat
import dev.lewisscoging.nightstand.settings.NightstandSettings
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Acceptance tests for behaviour settings changed while the clock is Visible: the settings UI
 * can't be reached while the dream covers the screen, so these stand in for a device check.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DreamControllerSettingsChangeTest {
    private class Fixture(
        private val testScope: TestScope,
        initialSettings: NightstandSettings,
        systemIs24Hour: Boolean,
        initialOrientation: Boolean
    ) {
        val orientation = MutableStateFlow(initialOrientation)
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
            orientation = orientation
        )

        val visible: Boolean get() = controller.uiState.value.clockVisible
        val timeText: String get() = controller.uiState.value.timeText

        init {
            testScope.backgroundScope.launch {
                controller.uiState.collect { state ->
                    if (visibilityChanges.lastOrNull()?.second != state.clockVisible) {
                        visibilityChanges +=
                            testScope.testScheduler.currentTime to state.clockVisible
                    }
                }
            }
            testScope.runCurrent()
        }

        fun change(transform: (NightstandSettings) -> NightstandSettings) {
            settings.update(transform)
            testScope.runCurrent()
        }

        fun tapAndSettle() {
            controller.onTap()
            testScope.runCurrent()
        }
    }

    private fun TestScope.fixture(
        settings: NightstandSettings = ALWAYS_ON,
        systemIs24Hour: Boolean = true,
        orientationOk: Boolean = true
    ) = Fixture(this, settings, systemIs24Hour, orientationOk)

    private fun TestScope.advanceAndSettle(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    @Test
    fun newTimeoutLeavesTheCurrentHideAndAppliesFromTheNextTap() = runTest {
        val fixture = fixture()
        fixture.tapAndSettle()
        advanceAndSettle(5_000)
        fixture.change { it.copy(clockTimeoutSeconds = 60) }
        assertTrue(fixture.visible)
        advanceAndSettle(9_999)
        assertTrue(fixture.visible)
        advanceAndSettle(1)
        assertFalse(fixture.visible)

        advanceAndSettle(5_000)
        fixture.tapAndSettle()
        advanceAndSettle(59_999)
        assertTrue(fixture.visible)
        advanceAndSettle(1)
        assertFalse(fixture.visible)
    }

    @Test
    fun shorterTimeoutDoesNotCutTheCurrentClockShort() = runTest {
        val fixture = fixture(ALWAYS_ON.copy(clockTimeoutSeconds = 60))
        fixture.tapAndSettle()
        advanceAndSettle(10_000)
        fixture.change { it.copy(clockTimeoutSeconds = 5) }
        advanceAndSettle(49_999)
        assertTrue(fixture.visible)
        advanceAndSettle(1)
        assertFalse(fixture.visible)

        fixture.tapAndSettle()
        advanceAndSettle(5_000)
        assertFalse(fixture.visible)
    }

    @Test
    fun disablingWhileVisibleHidesImmediatelyAndIgnoresTaps() = runTest {
        val fixture = fixture()
        fixture.tapAndSettle()
        advanceAndSettle(2_000)
        fixture.change { it.copy(enabled = false) }
        assertFalse(fixture.visible)
        assertEquals(listOf(0L to false, 0L to true, 2_000L to false), fixture.visibilityChanges)

        fixture.tapAndSettle()
        assertFalse(fixture.visible)
    }

    @Test
    fun reEnablingDoesNotShowTheClockUntilTheNextTap() = runTest {
        val fixture = fixture(ALWAYS_ON.copy(enabled = false))
        fixture.change { it.copy(enabled = true) }
        assertTrue(fixture.controller.tapsAllowed.value)
        assertFalse(fixture.visible)
        fixture.tapAndSettle()
        assertTrue(fixture.visible)
    }

    @Test
    fun windowMovedAwayFromNowHidesImmediately() = runTest {
        val fixture = fixture()
        fixture.tapAndSettle()
        advanceAndSettle(3_000)
        fixture.change { it.copy(startTime = TEN_AM_MINUTES, endTime = NOON_MINUTES) }
        assertFalse(fixture.visible)
        assertFalse(fixture.controller.tapsAllowed.value)

        fixture.tapAndSettle()
        assertFalse(fixture.visible)
    }

    @Test
    fun windowChangeThatStillCoversNowKeepsTheClockAndItsTimeout() = runTest {
        val fixture = fixture()
        fixture.tapAndSettle()
        advanceAndSettle(3_000)
        fixture.change { it.copy(startTime = MIDNIGHT_MINUTES, endTime = ONE_AM_MINUTES) }
        assertTrue(fixture.visible)
        advanceAndSettle(11_999)
        assertTrue(fixture.visible)
        advanceAndSettle(1)
        assertFalse(fixture.visible)
    }

    @Test
    fun requiringLandscapeWhileNotOnTheLongEdgeHidesImmediately() = runTest {
        val fixture = fixture(
            ALWAYS_ON.copy(requireLandscape = false),
            orientationOk = false
        )
        fixture.tapAndSettle()
        assertTrue(fixture.visible)
        advanceAndSettle(4_000)
        fixture.change { it.copy(requireLandscape = true) }
        assertFalse(fixture.visible)

        fixture.tapAndSettle()
        assertFalse(fixture.visible)
    }

    @Test
    fun requiringLandscapeWhileOnTheLongEdgeKeepsTheClock() = runTest {
        val fixture = fixture(ALWAYS_ON.copy(requireLandscape = false))
        fixture.tapAndSettle()
        advanceAndSettle(4_000)
        fixture.change { it.copy(requireLandscape = true) }
        assertTrue(fixture.visible)
        assertTrue(fixture.controller.tapsAllowed.value)
    }

    @Test
    fun droppingTheLandscapeRequirementReallowsTaps() = runTest {
        val fixture = fixture(orientationOk = false)
        fixture.tapAndSettle()
        assertFalse(fixture.visible)
        fixture.change { it.copy(requireLandscape = false) }
        assertTrue(fixture.controller.tapsAllowed.value)
        assertFalse(fixture.visible)
        fixture.tapAndSettle()
        assertTrue(fixture.visible)
    }

    @Test
    fun hourFormatChangeShowsOnTheNextTick() = runTest {
        val fixture = fixture(ALWAYS_ON.copy(hourFormat = HourFormat.H24))
        advanceAndSettle(THIRTEEN_OH_FIVE_MILLIS)
        fixture.tapAndSettle()
        assertEquals("13:05", fixture.timeText)

        advanceAndSettle(400)
        fixture.change { it.copy(hourFormat = HourFormat.H12) }
        assertEquals("13:05", fixture.timeText)
        advanceAndSettle(599)
        assertEquals("13:05", fixture.timeText)
        advanceAndSettle(1)
        assertEquals("1:05", fixture.timeText)
        assertTrue(fixture.visible)
    }

    @Test
    fun switchingBetweenSystemAndExplicitFormatsShowsOnTheNextTick() = runTest {
        val fixture = fixture(
            ALWAYS_ON.copy(hourFormat = HourFormat.SYSTEM),
            systemIs24Hour = false
        )
        advanceAndSettle(THIRTEEN_OH_FIVE_MILLIS)
        fixture.tapAndSettle()
        assertEquals("1:05", fixture.timeText)

        fixture.change { it.copy(hourFormat = HourFormat.H24) }
        advanceAndSettle(1_000)
        assertEquals("13:05", fixture.timeText)

        fixture.change { it.copy(hourFormat = HourFormat.SYSTEM) }
        advanceAndSettle(1_000)
        assertEquals("1:05", fixture.timeText)
    }

    @Test
    fun hourFormatChangeDoesNotMoveTheHide() = runTest {
        val fixture = fixture()
        fixture.tapAndSettle()
        advanceAndSettle(7_000)
        fixture.change { it.copy(hourFormat = HourFormat.H12) }
        advanceAndSettle(7_999)
        assertTrue(fixture.visible)
        advanceAndSettle(1)
        assertFalse(fixture.visible)
    }

    @Test
    fun twentyRapidTapsAcrossATimeoutChangeKeepOneTickerAndOneHide() = runTest {
        val fixture = fixture()
        repeat(RAPID_TAPS) { index ->
            if (index == RAPID_TAPS / 2) fixture.change { it.copy(clockTimeoutSeconds = 30) }
            fixture.tapAndSettle()
            advanceAndSettle(RAPID_TAP_INTERVAL_MILLIS)
        }
        val lastTapAt = (RAPID_TAPS - 1) * RAPID_TAP_INTERVAL_MILLIS
        assertEquals(1_900L, lastTapAt)

        val callsBefore = fixture.nowCalls
        advanceAndSettle(10_000)
        assertEquals(TICKS_IN_TEN_SECONDS, fixture.nowCalls - callsBefore)

        advanceAndSettle(lastTapAt + 30_000 - testScheduler.currentTime - 1)
        assertTrue(fixture.visible)
        advanceAndSettle(1)
        assertFalse(fixture.visible)
        assertEquals(
            listOf(0L to false, 0L to true, lastTapAt + 30_000 to false),
            fixture.visibilityChanges
        )
    }

    private companion object {
        const val TEST_DATE_PATTERN = "yyyy-MM-dd"
        const val MIDNIGHT_MINUTES = 0
        const val ONE_AM_MINUTES = 60
        const val TEN_AM_MINUTES = 600
        const val NOON_MINUTES = 720
        const val THIRTEEN_OH_FIVE_MILLIS = (13 * 60 + 5) * 60_000L
        const val RAPID_TAPS = 20
        const val RAPID_TAP_INTERVAL_MILLIS = 100L
        const val TICKS_IN_TEN_SECONDS = 10
        val ALWAYS_ON = NightstandSettings(startTime = MIDNIGHT_MINUTES, endTime = MIDNIGHT_MINUTES)
    }
}
