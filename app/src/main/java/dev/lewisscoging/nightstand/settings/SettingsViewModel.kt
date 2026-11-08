package dev.lewisscoging.nightstand.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

// One setter per persisted field is the screen's contract, so the function count grows with the model.
@Suppress("TooManyFunctions")
class SettingsViewModel(private val repository: SettingsRepository) :
    ViewModel(),
    SettingsActions {
    val settings: StateFlow<NightstandSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = NightstandSettings()
    )

    override fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    override fun setStartTime(minutesOfDay: Int) = update { it.copy(startTime = minutesOfDay) }

    override fun setEndTime(minutesOfDay: Int) = update { it.copy(endTime = minutesOfDay) }

    override fun setClockTimeoutSeconds(seconds: Int) = update {
        it.copy(clockTimeoutSeconds = seconds)
    }

    override fun setRequireLandscape(required: Boolean) = update {
        it.copy(requireLandscape = required)
    }

    override fun setUse24Hour(format: HourFormat) = update { it.copy(use24Hour = format) }

    override fun setClockFont(font: ClockFont) = update { it.copy(clockFont = font) }

    override fun setTextColor(color: TextColor) = update { it.copy(textColor = color) }

    override fun setBackgroundColor(color: BackgroundColor) = update {
        it.copy(backgroundColor = color)
    }

    override fun setClockBrightness(brightness: Float) = update {
        it.copy(clockBrightness = brightness)
    }

    override fun setShowDate(show: Boolean) = update { it.copy(showDate = show) }

    override fun setBurnInShift(shift: Boolean) = update { it.copy(burnInShift = shift) }

    private fun update(transform: (NightstandSettings) -> NightstandSettings) {
        viewModelScope.launch { repository.update(transform) }
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
