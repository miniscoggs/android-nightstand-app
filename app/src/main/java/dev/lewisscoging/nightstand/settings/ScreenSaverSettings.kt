package dev.lewisscoging.nightstand.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

private const val LOG_TAG = "ScreenSaverSettings"

/** Opens the system screen saver page, or the display page on builds that hide it. */
fun openScreenSaverSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
    } catch (dreamSettingsMissing: ActivityNotFoundException) {
        Log.w(
            LOG_TAG,
            "No screen saver settings page, opening display settings",
            dreamSettingsMissing
        )
        context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
    }
}
