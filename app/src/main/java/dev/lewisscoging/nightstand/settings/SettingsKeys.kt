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
    val USE_24_HOUR = stringPreferencesKey("use_24_hour")
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
    use24Hour = enumByName(this[SettingsKeys.USE_24_HOUR], DEFAULT_HOUR_FORMAT),
    clockFont = enumByName(this[SettingsKeys.CLOCK_FONT], DEFAULT_CLOCK_FONT),
    textColor = enumByName(this[SettingsKeys.TEXT_COLOR], DEFAULT_TEXT_COLOR),
    backgroundColor = enumByName(this[SettingsKeys.BACKGROUND_COLOR], DEFAULT_BACKGROUND_COLOR),
    clockBrightness = this[SettingsKeys.CLOCK_BRIGHTNESS] ?: DEFAULT_CLOCK_BRIGHTNESS,
    showDate = this[SettingsKeys.SHOW_DATE] ?: DEFAULT_SHOW_DATE,
    burnInShift = this[SettingsKeys.BURN_IN_SHIFT] ?: DEFAULT_BURN_IN_SHIFT
).sanitised()

fun MutablePreferences.writeSettings(settings: NightstandSettings) {
    this[SettingsKeys.ENABLED] = settings.enabled
    this[SettingsKeys.START_TIME] = settings.startTime
    this[SettingsKeys.END_TIME] = settings.endTime
    this[SettingsKeys.CLOCK_TIMEOUT_SECONDS] = settings.clockTimeoutSeconds
    this[SettingsKeys.REQUIRE_LANDSCAPE] = settings.requireLandscape
    this[SettingsKeys.USE_24_HOUR] = settings.use24Hour.name
    this[SettingsKeys.CLOCK_FONT] = settings.clockFont.name
    this[SettingsKeys.TEXT_COLOR] = settings.textColor.name
    this[SettingsKeys.BACKGROUND_COLOR] = settings.backgroundColor.name
    this[SettingsKeys.CLOCK_BRIGHTNESS] = settings.clockBrightness
    this[SettingsKeys.SHOW_DATE] = settings.showDate
    this[SettingsKeys.BURN_IN_SHIFT] = settings.burnInShift
}

private inline fun <reified T : Enum<T>> enumByName(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
