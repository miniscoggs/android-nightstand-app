package dev.lewisscoging.nightstand.dream

import dev.lewisscoging.nightstand.clock.ClockStyle
import dev.lewisscoging.nightstand.clock.NO_BURN_IN_OFFSET
import dev.lewisscoging.nightstand.clock.toClockStyle
import dev.lewisscoging.nightstand.settings.NightstandSettings

data class DreamUiState(
    val clockVisible: Boolean = false,
    val timeText: String = "",
    val dateText: String? = null,
    val style: ClockStyle = NightstandSettings().toClockStyle(NO_BURN_IN_OFFSET)
)
