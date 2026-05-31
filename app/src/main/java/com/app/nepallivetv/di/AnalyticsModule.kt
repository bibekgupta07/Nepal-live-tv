package com.app.nepallivetv.di

import com.app.nepallivetv.analytics.CrashReporter
import com.app.nepallivetv.analytics.RemoteConfigService
import com.app.nepallivetv.analytics.Telemetry
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import org.koin.dsl.module

val analyticsModule = module {
    single<FirebaseAnalytics> { Firebase.analytics }
    single<FirebaseCrashlytics> { FirebaseCrashlytics.getInstance() }
    single<FirebaseRemoteConfig> { Firebase.remoteConfig }

    single { Telemetry(get()) }
    single { CrashReporter(get()) }
    single { RemoteConfigService(get()) }
}
