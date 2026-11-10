package dev.lewisscoging.nightstand.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Reads as empty and fails every write with [error], as a full or unwritable disk would. */
class ThrowingDataStore(private val error: Throwable) : DataStore<Preferences> {
    override val data: Flow<Preferences> = flowOf(emptyPreferences())

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences = throw error
}
