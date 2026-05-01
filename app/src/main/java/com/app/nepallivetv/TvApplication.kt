package com.app.nepallivetv

import android.app.Application
import com.app.nepallivetv.di.appModule
import com.app.nepallivetv.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TvApplication)
            modules(listOf(networkModule, appModule))
        }
    }
}
