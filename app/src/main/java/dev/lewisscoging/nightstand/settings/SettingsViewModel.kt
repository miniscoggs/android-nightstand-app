package dev.lewisscoging.nightstand.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    /** Null until storage is first read, so the screen never shows stand-in defaults. */
    val settings: StateFlow<NightstandSettings?> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = null
    )

    // Buffered so a failure is still shown if it happens while the screen is not collecting.
    private val saveFailureEvents = Channel<Unit>(Channel.BUFFERED)

    /** Emits once per settings write that failed; each emission goes to one collector. */
    val saveFailures: Flow<Unit> = saveFailureEvents.receiveAsFlow()

    /** Persists [transform] applied to the stored settings; a failed write is reported on [saveFailures]. */
    fun update(transform: (NightstandSettings) -> NightstandSettings) {
        viewModelScope.launch {
            if (!repository.update(transform)) saveFailureEvents.send(Unit)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                SettingsViewModel(SettingsRepository(application.settingsDataStore))
            }
        }
    }
}
