package com.nfcraft.app

import android.app.Application
import com.nfcraft.app.di.AppContainer

class NfcraftApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
