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

    suspend fun update(transform: (NightstandSettings) -> NightstandSettings) {
        dataStore.edit { preferences ->
            preferences.writeSettings(transform(preferences.toNightstandSettings()).sanitised())
        }
    }
}
