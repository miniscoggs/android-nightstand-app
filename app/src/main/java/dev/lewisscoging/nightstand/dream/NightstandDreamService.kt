package dev.lewisscoging.nightstand.dream

import android.service.dreams.DreamService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

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
        dreamScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val composeView = ComposeView(this).apply { setContent { DreamContent() } }
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
private fun DreamContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black))
}
