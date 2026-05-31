package com.app.nepallivetv.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Thin wrapper over [FirebaseCrashlytics]. We want non-fatal exceptions to
 * still land in the Crashlytics dashboard — particularly scraper parse
 * failures — so we can spot upstream drift before users complain.
 */
class CrashReporter(private val crashlytics: FirebaseCrashlytics) {

    fun setEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    fun recordNonFatal(throwable: Throwable, tag: String? = null) {
        if (tag != null) crashlytics.setCustomKey("tag", tag)
        crashlytics.recordException(throwable)
    }

    fun log(message: String) {
        crashlytics.log(message)
    }
}
