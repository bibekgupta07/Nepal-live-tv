package com.app.nepallivetv

import android.app.Application
import com.app.nepallivetv.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TvApplication)
            modules(appModule)
        }
    }
}
