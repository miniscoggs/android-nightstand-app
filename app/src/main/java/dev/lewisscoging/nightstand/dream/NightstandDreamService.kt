package dev.lewisscoging.nightstand.dream

import android.hardware.SensorManager
import android.service.dreams.DreamService
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.lewisscoging.nightstand.clock.ClockFace
import dev.lewisscoging.nightstand.core.systemNowProvider
import dev.lewisscoging.nightstand.settings.SettingsRepository
import dev.lewisscoging.nightstand.settings.settingsDataStore
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

private const val DATE_SKELETON = "EEEdMMM"

class NightstandDreamService : DreamService() {
    private val dreamLifecycleOwner = DreamLifecycleOwner()
    private var dreamScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        dreamLifecycleOwner.onCreate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true
        isScreenBright = false
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        dreamScope = scope
        val controller = DreamController(
            scope = scope,
            settings = SettingsRepository(applicationContext.settingsDataStore).settings,
            nowProvider = systemNowProvider,
            systemIs24Hour = { DateFormat.is24HourFormat(this) },
            datePattern = { DateFormat.getBestDateTimePattern(Locale.getDefault(), DATE_SKELETON) },
            orientation = OrientationMonitor(getSystemService(SensorManager::class.java))
                .restingOnLongEdge()
        )
        val composeView = ComposeView(this).apply { setContent { DreamContent(controller) } }
        listOf(composeView, window.decorView).forEach { view ->
            view.setViewTreeLifecycleOwner(dreamLifecycleOwner)
            view.setViewTreeSavedStateRegistryOwner(dreamLifecycleOwner)
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        dreamLifecycleOwner.onDreamingStarted()
    }

    override fun onDreamingStopped() {
        dreamLifecycleOwner.onDreamingStopped()
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        dreamScope?.cancel()
        dreamScope = null
        super.onDetachedFromWindow()
    }

    override fun onDestroy() {
        dreamLifecycleOwner.onDestroy()
        super.onDestroy()
    }
}

@Composable
private fun DreamContent(controller: DreamController, modifier: Modifier = Modifier) {
    val uiState by controller.uiState.collectAsStateWithLifecycle()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(uiState.style.backgroundArgb))
            .pointerInput(controller) { detectTapGestures(onTap = { controller.onTap() }) }
    ) {
        ClockFace(
            visible = uiState.clockVisible,
            timeText = uiState.timeText,
            dateText = uiState.dateText,
            style = uiState.style
        )
    }
}
