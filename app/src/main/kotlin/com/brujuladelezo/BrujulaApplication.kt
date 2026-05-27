package com.brujuladelezo

import android.app.Application
import com.brujuladelezo.di.AppContainer

class BrujulaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
