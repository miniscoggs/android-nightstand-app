package dev.lewisscoging.nightstand.settings

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun viewModelTest(block: suspend TestScope.(SettingsViewModel) -> Unit) = runTest {
        val store = PreferenceDataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
                tempFolder.root.resolve("test.preferences_pb").toOkioPath()
            },
            scope = backgroundScope
        )
        val viewModel = SettingsViewModel(SettingsRepository(store))
        backgroundScope.launch { viewModel.settings.collect {} }
        block(viewModel)
    }

    private fun assertUpdates(expected: NightstandSettings, action: SettingsViewModel.() -> Unit) =
        viewModelTest { viewModel ->
            viewModel.action()
            assertEquals(expected, viewModel.settings.first { it == expected })
        }

    @Test
    fun initialStateIsTheDefaults() = viewModelTest { viewModel ->
        assertEquals(NightstandSettings(), viewModel.settings.value)
    }

    @Test
    fun setEnabledUpdatesState() =
        assertUpdates(NightstandSettings(enabled = false)) { setEnabled(false) }

    @Test
    fun setStartTimeUpdatesState() =
        assertUpdates(NightstandSettings(startTime = 21 * 60)) { setStartTime(21 * 60) }

    @Test
    fun setEndTimeUpdatesState() =
        assertUpdates(NightstandSettings(endTime = 6 * 60 + 30)) { setEndTime(6 * 60 + 30) }

    @Test
    fun setClockTimeoutSecondsUpdatesState() =
        assertUpdates(NightstandSettings(clockTimeoutSeconds = 45)) { setClockTimeoutSeconds(45) }

    @Test
    fun setRequireLandscapeUpdatesState() =
        assertUpdates(NightstandSettings(requireLandscape = false)) { setRequireLandscape(false) }

    @Test
    fun setUse24HourUpdatesState() = assertUpdates(NightstandSettings(use24Hour = HourFormat.H12)) {
        setUse24Hour(HourFormat.H12)
    }

    @Test
    fun setClockFontUpdatesState() =
        assertUpdates(NightstandSettings(clockFont = ClockFont.BUBBLE)) {
            setClockFont(ClockFont.BUBBLE)
        }

    @Test
    fun setTextColorUpdatesState() =
        assertUpdates(NightstandSettings(textColor = TextColor.AMBER)) {
            setTextColor(TextColor.AMBER)
        }

    @Test
    fun setBackgroundColorUpdatesState() =
        assertUpdates(NightstandSettings(backgroundColor = BackgroundColor.MIDNIGHT_BLUE)) {
            setBackgroundColor(BackgroundColor.MIDNIGHT_BLUE)
        }

    @Test
    fun setClockBrightnessUpdatesState() =
        assertUpdates(NightstandSettings(clockBrightness = 0.6f)) { setClockBrightness(0.6f) }

    @Test
    fun setShowDateUpdatesState() =
        assertUpdates(NightstandSettings(showDate = true)) { setShowDate(true) }

    @Test
    fun setBurnInShiftUpdatesState() =
        assertUpdates(NightstandSettings(burnInShift = false)) { setBurnInShift(false) }

    @Test
    fun eachFailedWriteEmitsOneSaveFailure() = runTest {
        val viewModel =
            SettingsViewModel(SettingsRepository(ThrowingDataStore(IOException("full"))))
        val failures = collectSaveFailures(viewModel)
        viewModel.setShowDate(true)
        viewModel.setBurnInShift(false)
        advanceUntilIdle()
        assertEquals(2, failures.size)
    }

    @Test
    fun successfulWriteEmitsNoSaveFailure() = viewModelTest { viewModel ->
        val failures = collectSaveFailures(viewModel)
        val expected = NightstandSettings(showDate = true)
        viewModel.setShowDate(true)
        viewModel.settings.first { it == expected }
        advanceUntilIdle()
        assertEquals(0, failures.size)
    }
}
