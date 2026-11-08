package dev.lewisscoging.nightstand.dream

import dev.lewisscoging.nightstand.core.NowProvider
import dev.lewisscoging.nightstand.settings.HourFormat
import dev.lewisscoging.nightstand.settings.NightstandSettings
import java.time.Instant
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
class DreamControllerTest {
    private class Fixture(testScope: TestScope, initialSettings: NightstandSettings) {
        val settings = MutableStateFlow(initialSettings)
        val visibilityChanges = mutableListOf<Pair<Long, Boolean>>()
        val controller = DreamController(
            scope = testScope.backgroundScope,
            settings = settings,
            nowProvider = NowProvider {
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(testScope.testScheduler.currentTime),
                    ZoneOffset.UTC
                )
            },
            systemIs24Hour = { true }
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

    private fun TestScope.fixture(timeoutSeconds: Int = 15) = Fixture(
        this,
        NightstandSettings(clockTimeoutSeconds = timeoutSeconds, use24Hour = HourFormat.H24)
    )

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
}
