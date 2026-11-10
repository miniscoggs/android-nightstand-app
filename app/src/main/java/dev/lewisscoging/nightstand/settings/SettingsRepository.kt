package dev.lewisscoging.nightstand.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Build from `context.applicationContext.settingsDataStore`. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    /** The current settings; a read failure gives the defaults. */
    val settings: Flow<NightstandSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toNightstandSettings() }
        .distinctUntilChanged()

    /**
     * Applies [transform] and writes only the fields it changed. Returns false when the write
     * fails with an [IOException] (the caller reports it); any other exception is a bug and
     * propagates.
     */
    suspend fun update(transform: (NightstandSettings) -> NightstandSettings): Boolean = try {
        dataStore.edit { preferences ->
            val before = preferences.toNightstandSettings()
            preferences.writeChangedSettings(before, transform(before).sanitised())
        }
        true
    } catch (_: IOException) {
        false
    }
}
