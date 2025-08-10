package com.tambapps.pokemon.alakastats

import android.app.Application
import com.tambapps.pokemon.alakastats.di.allModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AlakastatsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@AlakastatsApplication)
            modules(allModules)
        }
    }
}