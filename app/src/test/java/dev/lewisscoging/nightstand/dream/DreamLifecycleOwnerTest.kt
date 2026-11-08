package dev.lewisscoging.nightstand.dream

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DreamLifecycleOwnerTest {
    private val owner =
        DreamLifecycleOwner { lifecycleOwner -> LifecycleRegistry.createUnsafe(lifecycleOwner) }

    @Test
    fun startsInitialised() {
        assertEquals(Lifecycle.State.INITIALIZED, owner.lifecycle.currentState)
        assertFalse(owner.savedStateRegistry.isRestored)
    }

    @Test
    fun isCreatedAfterOnCreate() {
        owner.onCreate()
        assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)
    }

    @Test
    fun savedStateIsRestoredBeforeOnCreate() {
        var restoredAtCreate = false
        owner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_CREATE) {
                    restoredAtCreate = owner.savedStateRegistry.isRestored
                }
            }
        )
        owner.onCreate()
        assertTrue(restoredAtCreate)
        assertTrue(owner.savedStateRegistry.isRestored)
    }

    @Test
    fun isResumedAfterDreamingStarts() {
        owner.onCreate()
        owner.onDreamingStarted()
        assertEquals(Lifecycle.State.RESUMED, owner.lifecycle.currentState)
    }

    @Test
    fun isCreatedAfterDreamingStops() {
        owner.onCreate()
        owner.onDreamingStarted()
        owner.onDreamingStopped()
        assertEquals(Lifecycle.State.CREATED, owner.lifecycle.currentState)
    }

    @Test
    fun isDestroyedAfterOnDestroy() {
        owner.onCreate()
        owner.onDreamingStarted()
        owner.onDreamingStopped()
        owner.onDestroy()
        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
    }
}
