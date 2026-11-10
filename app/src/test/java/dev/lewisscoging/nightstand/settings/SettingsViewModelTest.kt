package dev.lewisscoging.nightstand.settings

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Uses okio storage for the reason given in [SettingsRepositoryTest]. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.collectSaveFailures(viewModel: SettingsViewModel): List<Unit> {
        val failures = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.saveFailures.collect { failures += it }
        }
        return failures
    }

    private fun TestScope.newRepository() = SettingsRepository(
        PreferenceDataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
                tempFolder.root.resolve("test.preferences_pb").toOkioPath()
            },
            scope = backgroundScope
        )
    )

    private fun viewModelTest(block: suspend TestScope.(SettingsViewModel) -> Unit) = runTest {
        val viewModel = SettingsViewModel(newRepository())
        backgroundScope.launch { viewModel.settings.collect {} }
        block(viewModel)
    }

    @Test
    fun firstLoadedStateIsTheDefaults() = viewModelTest { viewModel ->
        assertEquals(NightstandSettings(), viewModel.settings.filterNotNull().first())
    }

    @Test
    fun initialStateIsNullAndTheStoredValueFollows() = runTest {
        val repository = newRepository()
        val stored = NightstandSettings(showDate = true)
        repository.update { stored }
        val viewModel = SettingsViewModel(repository)
        assertNull(viewModel.settings.value)
        assertEquals(stored, viewModel.settings.filterNotNull().first())
    }

    @Test
    fun updateAppliesTheTransformToTheStoredValue() = runTest {
        val repository = newRepository()
        repository.update { it.copy(showDate = true, clockTimeoutSeconds = 30) }
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch { viewModel.settings.collect {} }
        viewModel.update { it.copy(clockTimeoutSeconds = it.clockTimeoutSeconds + 15) }
        val expected = NightstandSettings(showDate = true, clockTimeoutSeconds = 45)
        assertEquals(expected, viewModel.settings.first { it == expected })
        assertEquals(expected, repository.settings.first())
    }

    @Test
    fun updateSanitisesOutOfRangeResults() = viewModelTest { viewModel ->
        viewModel.update {
            it.copy(
                clockTimeoutSeconds = MAX_CLOCK_TIMEOUT_SECONDS + 1,
                clockBrightness = MIN_CLOCK_BRIGHTNESS / 2
            )
        }
        val expected = NightstandSettings(
            clockTimeoutSeconds = MAX_CLOCK_TIMEOUT_SECONDS,
            clockBrightness = MIN_CLOCK_BRIGHTNESS
        )
        assertEquals(expected, viewModel.settings.first { it == expected })
    }

    @Test
    fun eachFailedWriteEmitsOneSaveFailure() = runTest {
        val viewModel =
            SettingsViewModel(SettingsRepository(ThrowingDataStore(IOException("full"))))
        val failures = collectSaveFailures(viewModel)
        viewModel.update { it.copy(showDate = true) }
        viewModel.update { it.copy(burnInShift = false) }
        advanceUntilIdle()
        assertEquals(2, failures.size)
    }

    @Test
    fun successfulWriteEmitsNoSaveFailure() = viewModelTest { viewModel ->
        val failures = collectSaveFailures(viewModel)
        val expected = NightstandSettings(showDate = true)
        viewModel.update { it.copy(showDate = true) }
        viewModel.settings.first { it == expected }
        advanceUntilIdle()
        assertEquals(0, failures.size)
    }
}
