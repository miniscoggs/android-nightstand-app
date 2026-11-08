package dev.lewisscoging.nightstand.dream

import android.graphics.Color
import android.service.dreams.DreamService
import android.view.View

class NightstandDreamService : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true
        isScreenBright = false
        setContentView(View(this).apply { setBackgroundColor(Color.BLACK) })
    }
}
