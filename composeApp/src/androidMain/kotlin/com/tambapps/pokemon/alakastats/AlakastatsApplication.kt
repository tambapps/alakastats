package com.tambapps.pokemon.alakastats

import android.app.Application
import com.tambapps.pokemon.alakastats.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AlakastatsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@AlakastatsApplication)
            modules(appModules)
        }
    }
}