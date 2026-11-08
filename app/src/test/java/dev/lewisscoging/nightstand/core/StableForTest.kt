package dev.lewisscoging.nightstand.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StableForTest {
    private class Recorder(private val testScope: TestScope) {
        val source = MutableSharedFlow<Boolean>()
        val emissions = mutableListOf<Pair<Long, Boolean>>()

        init {
            testScope.backgroundScope.launch {
                source.stableFor(ORIENTATION_STABLE_MS).collect { value ->
                    emissions += testScope.testScheduler.currentTime to value
                }
            }
            testScope.runCurrent()
        }

        suspend fun send(value: Boolean) {
            source.emit(value)
            testScope.runCurrent()
        }
    }

    @Test
    fun firstValueIsEmittedAfterTheStablePeriod() = runTest {
        val recorder = Recorder(this)
        recorder.send(true)
        advanceTimeBy(ORIENTATION_STABLE_MS - 1)
        runCurrent()
        assertEquals(emptyList<Pair<Long, Boolean>>(), recorder.emissions)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(ORIENTATION_STABLE_MS to true), recorder.emissions)
    }

    @Test
    fun shortFlickersAreIgnored() = runTest {
        val recorder = Recorder(this)
        recorder.send(true)
        advanceTimeBy(ORIENTATION_STABLE_MS)
        runCurrent()
        recorder.send(false)
        advanceTimeBy(FLICKER_MS)
        recorder.send(true)
        advanceTimeBy(FLICKER_MS)
        recorder.send(false)
        advanceTimeBy(FLICKER_MS)
        recorder.send(true)
        advanceTimeBy(ORIENTATION_STABLE_MS * 3)
        runCurrent()
        assertEquals(listOf(ORIENTATION_STABLE_MS to true), recorder.emissions)
    }

    @Test
    fun aChangeThatHoldsForTheStablePeriodIsEmitted() = runTest {
        val recorder = Recorder(this)
        recorder.send(true)
        advanceTimeBy(ORIENTATION_STABLE_MS)
        runCurrent()
        recorder.send(false)
        val changedAt = testScheduler.currentTime
        advanceTimeBy(ORIENTATION_STABLE_MS)
        runCurrent()
        assertEquals(
            listOf(ORIENTATION_STABLE_MS to true, changedAt + ORIENTATION_STABLE_MS to false),
            recorder.emissions
        )
    }

    @Test
    fun repeatedEqualReadingsDoNotRestartTheStablePeriod() = runTest {
        val recorder = Recorder(this)
        recorder.send(true)
        advanceTimeBy(FLICKER_MS)
        recorder.send(true)
        advanceTimeBy(ORIENTATION_STABLE_MS - FLICKER_MS)
        runCurrent()
        assertEquals(listOf(ORIENTATION_STABLE_MS to true), recorder.emissions)
    }

    private companion object {
        const val FLICKER_MS = 300L
    }
}
