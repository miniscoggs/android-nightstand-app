package dev.lewisscoging.nightstand.settings

/** The settings screen's write side: one setter per persisted field. */
// One setter per persisted field, so the function count grows with the model.
@Suppress("TooManyFunctions")
interface SettingsActions {
    fun setEnabled(enabled: Boolean)

    fun setStartTime(minutesOfDay: Int)

    fun setEndTime(minutesOfDay: Int)

    fun setClockTimeoutSeconds(seconds: Int)

    fun setRequireLandscape(required: Boolean)

    fun setUse24Hour(format: HourFormat)

    fun setClockFont(font: ClockFont)

    fun setTextColor(color: TextColor)

    fun setBackgroundColor(color: BackgroundColor)

    fun setClockBrightness(brightness: Float)

    fun setShowDate(show: Boolean)

    fun setBurnInShift(shift: Boolean)
}
