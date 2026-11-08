package dev.lewisscoging.nightstand.dream

data class DreamUiState(
    val clockVisible: Boolean = false,
    val timeText: String = "",
    val dateText: String? = null
)
