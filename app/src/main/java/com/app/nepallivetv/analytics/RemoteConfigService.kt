package com.app.nepallivetv.analytics

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

/**
 * Wraps Firebase Remote Config with our compiled-in defaults. The pattern
 * is: every value has a constant default that always works; Remote Config
 * is an optional override pushed from the Firebase console when we need to
 * change something at runtime without an APK release.
 *
 * Common motivations:
 *  - Upstream domain changes (lookmovie moves to a new TLD)
 *  - Tightening or relaxing scraper timeouts
 *  - Feature flags for experimental UI
 */
class RemoteConfigService(private val rc: FirebaseRemoteConfig) {

    init {
        rc.setConfigSettingsAsync(
            remoteConfigSettings {
                // Production-ish: re-fetch at most once per hour. Override via
                // `fetchAndActivate()` directly during testing.
                minimumFetchIntervalInSeconds = 3600
            },
        )
        rc.setDefaultsAsync(
            mapOf(
                KEY_LOOKMOVIE_BASE_URL to DEFAULT_LOOKMOVIE_BASE_URL,
                KEY_TECHJAIL_BASE_URL to DEFAULT_TECHJAIL_BASE_URL,
                KEY_SCRAPER_TIMEOUT_MS to DEFAULT_SCRAPER_TIMEOUT_MS,
            ),
        )
    }

    fun fetchAndActivateAsync() {
        rc.fetchAndActivate().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "remote config fetch failed", task.exception)
            }
        }
    }

    val lookmovieBaseUrl: String
        get() = rc.getString(KEY_LOOKMOVIE_BASE_URL)
            .ifBlank { DEFAULT_LOOKMOVIE_BASE_URL }

    val techjailBaseUrl: String
        get() = rc.getString(KEY_TECHJAIL_BASE_URL)
            .ifBlank { DEFAULT_TECHJAIL_BASE_URL }

    val scraperTimeoutMs: Long
        get() = rc.getLong(KEY_SCRAPER_TIMEOUT_MS).takeIf { it > 0 }
            ?: DEFAULT_SCRAPER_TIMEOUT_MS

    private companion object {
        const val TAG = "RemoteConfig"

        const val KEY_LOOKMOVIE_BASE_URL = "lookmovie_base_url"
        const val KEY_TECHJAIL_BASE_URL = "techjail_base_url"
        const val KEY_SCRAPER_TIMEOUT_MS = "scraper_timeout_ms"

        const val DEFAULT_LOOKMOVIE_BASE_URL = "https://lookmovie2.to"
        const val DEFAULT_TECHJAIL_BASE_URL = "http://tv.techjail.net/huritv9"
        const val DEFAULT_SCRAPER_TIMEOUT_MS = 20_000L
    }
}
