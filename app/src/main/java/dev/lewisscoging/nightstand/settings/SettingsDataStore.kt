package dev.lewisscoging.nightstand.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val SETTINGS_DATASTORE_NAME = "nightstand_settings"

/** The single per-process settings store; never construct a second one over the same file. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    SETTINGS_DATASTORE_NAME
)
