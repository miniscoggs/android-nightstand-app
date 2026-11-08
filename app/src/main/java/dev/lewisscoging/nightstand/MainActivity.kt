package dev.lewisscoging.nightstand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.lewisscoging.nightstand.settings.SettingsScreen
import dev.lewisscoging.nightstand.settings.theme.NightstandTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NightstandTheme {
                SettingsScreen()
            }
        }
    }
}
