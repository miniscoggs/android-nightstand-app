package dev.lewisscoging.nightstand.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** One preferences key per settings field; enums are stored by name, times as minutes since midnight. */
object SettingsKeys {
    val ENABLED = booleanPreferencesKey("enabled")
    val START_TIME = intPreferencesKey("start_time")
    val END_TIME = intPreferencesKey("end_time")
    val CLOCK_TIMEOUT_SECONDS = intPreferencesKey("clock_timeout_seconds")
    val REQUIRE_LANDSCAPE = booleanPreferencesKey("require_landscape")
    val HOUR_FORMAT = stringPreferencesKey("hour_format")
    val CLOCK_FONT = stringPreferencesKey("clock_font")
    val TEXT_COLOR = stringPreferencesKey("text_color")
    val BACKGROUND_COLOR = stringPreferencesKey("background_color")
    val CLOCK_BRIGHTNESS = floatPreferencesKey("clock_brightness")
    val SHOW_DATE = booleanPreferencesKey("show_date")
    val BURN_IN_SHIFT = booleanPreferencesKey("burn_in_shift")
}

/** Missing values and unknown enum names fall back to the defaults; out-of-range values are clamped. */
fun Preferences.toNightstandSettings(): NightstandSettings = NightstandSettings(
    enabled = this[SettingsKeys.ENABLED] ?: DEFAULT_ENABLED,
    startTime = this[SettingsKeys.START_TIME] ?: DEFAULT_START_MINUTES,
    endTime = this[SettingsKeys.END_TIME] ?: DEFAULT_END_MINUTES,
    clockTimeoutSeconds = this[SettingsKeys.CLOCK_TIMEOUT_SECONDS] ?: DEFAULT_CLOCK_TIMEOUT_SECONDS,
    requireLandscape = this[SettingsKeys.REQUIRE_LANDSCAPE] ?: DEFAULT_REQUIRE_LANDSCAPE,
    hourFormat = enumByName(this[SettingsKeys.HOUR_FORMAT], DEFAULT_HOUR_FORMAT),
    clockFont = enumByName(this[SettingsKeys.CLOCK_FONT], DEFAULT_CLOCK_FONT),
    textColor = enumByName(this[SettingsKeys.TEXT_COLOR], DEFAULT_TEXT_COLOR),
    backgroundColor = enumByName(this[SettingsKeys.BACKGROUND_COLOR], DEFAULT_BACKGROUND_COLOR),
    clockBrightness = this[SettingsKeys.CLOCK_BRIGHTNESS] ?: DEFAULT_CLOCK_BRIGHTNESS,
    showDate = this[SettingsKeys.SHOW_DATE] ?: DEFAULT_SHOW_DATE,
    burnInShift = this[SettingsKeys.BURN_IN_SHIFT] ?: DEFAULT_BURN_IN_SHIFT
).sanitised()

/**
 * Writes only the fields that differ between [before] and [after], so a key the user never
 * changed stays absent and keeps following the current default.
 */
fun MutablePreferences.writeChangedSettings(before: NightstandSettings, after: NightstandSettings) {
    writeIfChanged(SettingsKeys.ENABLED, before.enabled, after.enabled)
    writeIfChanged(SettingsKeys.START_TIME, before.startTime, after.startTime)
    writeIfChanged(SettingsKeys.END_TIME, before.endTime, after.endTime)
    writeIfChanged(
        SettingsKeys.CLOCK_TIMEOUT_SECONDS,
        before.clockTimeoutSeconds,
        after.clockTimeoutSeconds
    )
    writeIfChanged(SettingsKeys.REQUIRE_LANDSCAPE, before.requireLandscape, after.requireLandscape)
    writeIfChanged(SettingsKeys.HOUR_FORMAT, before.hourFormat.name, after.hourFormat.name)
    writeIfChanged(SettingsKeys.CLOCK_FONT, before.clockFont.name, after.clockFont.name)
    writeIfChanged(SettingsKeys.TEXT_COLOR, before.textColor.name, after.textColor.name)
    writeIfChanged(
        SettingsKeys.BACKGROUND_COLOR,
        before.backgroundColor.name,
        after.backgroundColor.name
    )
    writeIfChanged(SettingsKeys.CLOCK_BRIGHTNESS, before.clockBrightness, after.clockBrightness)
    writeIfChanged(SettingsKeys.SHOW_DATE, before.showDate, after.showDate)
    writeIfChanged(SettingsKeys.BURN_IN_SHIFT, before.burnInShift, after.burnInShift)
}

private fun <T> MutablePreferences.writeIfChanged(key: Preferences.Key<T>, before: T, after: T) {
    if (before != after) this[key] = after
}

private inline fun <reified T : Enum<T>> enumByName(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
