package com.brujuladelezo.di

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface

/**
 * Rotación actual del display por defecto (`Surface.ROTATION_*`).
 *
 * No se usa `Context.getDisplay()` (API 30+) porque exige un contexto *visual*: sobre el
 * Application context —que es el que construye [AppContainer]— lanza
 * `UnsupportedOperationException`. `DisplayManager` sí es utilizable desde cualquier contexto,
 * y evita el ya deprecado `WindowManager.getDefaultDisplay()`.
 */
fun defaultDisplayRotation(context: Context): Int {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    return displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
}
