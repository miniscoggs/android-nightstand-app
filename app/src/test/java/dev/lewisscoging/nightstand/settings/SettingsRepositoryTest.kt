package dev.lewisscoging.nightstand.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Uses okio storage: the File storage that every factory method uses on Android renames over the
 * existing file on every write after the first, which fails on Windows JVMs (devices are unaffected).
 */
class SettingsRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
                tempFolder.root.resolve("test.preferences_pb").toOkioPath()
            },
            scope = scope
        )

    private fun repositoryTest(
        block: suspend TestScope.(DataStore<Preferences>, SettingsRepository) -> Unit
    ) = runTest {
        val store = createStore(backgroundScope)
        block(store, SettingsRepository(store))
    }

    private fun assertRoundTrips(expected: NightstandSettings) = repositoryTest { _, repository ->
        repository.update { expected }
        assertEquals(expected, repository.settings.first())
    }

    @Test
    fun emptyStoreGivesTheDefaults() = repositoryTest { _, repository ->
        assertEquals(NightstandSettings(), repository.settings.first())
    }

    @Test
    fun emptyPreferencesMapToTheDefaults() {
        assertEquals(NightstandSettings(), emptyPreferences().toNightstandSettings())
    }

    @Test
    fun enabledRoundTrips() = assertRoundTrips(NightstandSettings(enabled = false))

    @Test
    fun startTimeRoundTrips() = assertRoundTrips(NightstandSettings(startTime = 21 * 60 + 30))

    @Test
    fun endTimeRoundTrips() = assertRoundTrips(NightstandSettings(endTime = 6 * 60 + 15))

    @Test
    fun clockTimeoutSecondsRoundTrips() =
        assertRoundTrips(NightstandSettings(clockTimeoutSeconds = 60))

    @Test
    fun requireLandscapeRoundTrips() =
        assertRoundTrips(NightstandSettings(requireLandscape = false))

    @Test
    fun use24HourRoundTrips() = assertRoundTrips(NightstandSettings(use24Hour = HourFormat.H24))

    @Test
    fun clockFontRoundTrips() = assertRoundTrips(NightstandSettings(clockFont = ClockFont.DIGITAL))

    @Test
    fun textColorRoundTrips() = assertRoundTrips(NightstandSettings(textColor = TextColor.AMBER))

    @Test
    fun backgroundColorRoundTrips() =
        assertRoundTrips(NightstandSettings(backgroundColor = BackgroundColor.MIDNIGHT_BLUE))

    @Test
    fun clockBrightnessRoundTrips() = assertRoundTrips(NightstandSettings(clockBrightness = 0.75f))

    @Test
    fun showDateRoundTrips() = assertRoundTrips(NightstandSettings(showDate = true))

    @Test
    fun burnInShiftRoundTrips() = assertRoundTrips(NightstandSettings(burnInShift = false))

    @Test
    fun unknownEnumNamesFallBackToTheDefaults() = repositoryTest { store, repository ->
        store.edit {
            it[SettingsKeys.USE_24_HOUR] = "H36"
            it[SettingsKeys.CLOCK_FONT] = "COMIC"
            it[SettingsKeys.TEXT_COLOR] = "PURPLE"
            it[SettingsKeys.BACKGROUND_COLOR] = ""
        }
        val settings = repository.settings.first()
        assertEquals(DEFAULT_HOUR_FORMAT, settings.use24Hour)
        assertEquals(DEFAULT_CLOCK_FONT, settings.clockFont)
        assertEquals(DEFAULT_TEXT_COLOR, settings.textColor)
        assertEquals(DEFAULT_BACKGROUND_COLOR, settings.backgroundColor)
    }

    @Test
    fun outOfRangeStoredValuesAreClampedUpwards() = repositoryTest { store, repository ->
        store.edit {
            it[SettingsKeys.START_TIME] = -1
            it[SettingsKeys.END_TIME] = -60
            it[SettingsKeys.CLOCK_TIMEOUT_SECONDS] = 1
            it[SettingsKeys.CLOCK_BRIGHTNESS] = 0f
        }
        val settings = repository.settings.first()
        assertEquals(0, settings.startTime)
        assertEquals(0, settings.endTime)
        assertEquals(MIN_CLOCK_TIMEOUT_SECONDS, settings.clockTimeoutSeconds)
        assertEquals(MIN_CLOCK_BRIGHTNESS, settings.clockBrightness, 0f)
    }

    @Test
    fun outOfRangeStoredValuesAreClampedDownwards() = repositoryTest { store, repository ->
        store.edit {
            it[SettingsKeys.START_TIME] = 1440
            it[SettingsKeys.END_TIME] = 5000
            it[SettingsKeys.CLOCK_TIMEOUT_SECONDS] = 999
            it[SettingsKeys.CLOCK_BRIGHTNESS] = 2f
        }
        val settings = repository.settings.first()
        assertEquals(1439, settings.startTime)
        assertEquals(1439, settings.endTime)
        assertEquals(MAX_CLOCK_TIMEOUT_SECONDS, settings.clockTimeoutSeconds)
        assertEquals(MAX_CLOCK_BRIGHTNESS, settings.clockBrightness, 0f)
    }

    @Test
    fun updateIsAppliedToTheCurrentSettingsAndEmitted() = repositoryTest { _, repository ->
        repository.update { it.copy(showDate = true) }
        repository.update { it.copy(clockTimeoutSeconds = 30) }
        assertEquals(
            NightstandSettings(showDate = true, clockTimeoutSeconds = 30),
            repository.settings.first()
        )
    }

    @Test
    fun updateClampsOutOfRangeValuesBeforeWriting() = repositoryTest { store, repository ->
        repository.update { it.copy(clockTimeoutSeconds = 500) }
        assertEquals(
            MAX_CLOCK_TIMEOUT_SECONDS,
            store.data.first()[SettingsKeys.CLOCK_TIMEOUT_SECONDS]
        )
    }
}
