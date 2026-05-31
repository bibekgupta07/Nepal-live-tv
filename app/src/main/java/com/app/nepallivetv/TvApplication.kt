package com.app.nepallivetv

import android.app.Application
import com.app.nepallivetv.analytics.CrashReporter
import com.app.nepallivetv.analytics.RemoteConfigService
import com.app.nepallivetv.analytics.Telemetry
import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import com.app.nepallivetv.di.analyticsModule
import com.app.nepallivetv.di.appModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TvApplication : Application() {

    // Koin-injected singletons used during startup. Pulled lazily to avoid
    // touching the graph before startKoin returns.
    private val telemetry: Telemetry by inject()
    private val crashReporter: CrashReporter by inject()
    private val remoteConfig: RemoteConfigService by inject()
    private val prefs: DatastorePreferences by inject()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@TvApplication)
            modules(listOf(appModule, analyticsModule))
        }

        // Seed Analytics + Crashlytics from the user's stored preference. The
        // DataStore read is suspending, so we do it off the main thread; until
        // it resolves, the SDK defaults to enabled, matching our default.
        appScope.launch {
            val enabled = prefs.isAnalyticsEnabled()
            telemetry.setEnabled(enabled)
            crashReporter.setEnabled(enabled)
        }

        // Fetch fresh Remote Config in the background. Falls back to compiled
        // defaults if it never returns.
        remoteConfig.fetchAndActivateAsync()
    }
}
