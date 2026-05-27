package com.brujuladelezo.app

import android.app.Application
import com.brujuladelezo.app.di.AppContainer

class BrujulaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
