package com.app.nepallivetv.analytics

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin typed wrapper over [FirebaseAnalytics]. Centralizes event names and
 * payload shapes so we never sprinkle bare strings through ViewModels.
 *
 * Call sites:
 *  - [mediaPlayStarted]    MoviePlayerViewModel.load() success
 *  - [mediaPlayFailed]     MoviePlayerViewModel.load() failure + retry
 *  - [searchPerformed]     MoviesViewModel.search() after debounce fires
 *  - [channelPlayStarted]  SharedViewModel.selectChannel() stream resolved
 *  - [updateAccepted]      UpdateViewModel.downloadAndInstall()
 *  - [scraperDriftDetected] anywhere we hit an unexpected upstream layout
 */
class Telemetry(private val analytics: FirebaseAnalytics) {

    fun setEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    fun mediaPlayStarted(kind: String, title: String, source: String = SOURCE_LOOKMOVIE) {
        analytics.logEvent(
            EVT_MEDIA_PLAY_STARTED,
            bundleOf(
                P_KIND to kind,
                P_TITLE to title.take(100),
                P_SOURCE to source,
            ),
        )
    }

    fun mediaPlayFailed(kind: String, reason: String) {
        analytics.logEvent(
            EVT_MEDIA_PLAY_FAILED,
            bundleOf(
                P_KIND to kind,
                P_REASON to reason.take(100),
            ),
        )
    }

    fun searchPerformed(queryLength: Int, resultCount: Int) {
        analytics.logEvent(
            EVT_SEARCH_PERFORMED,
            bundleOf(
                P_QUERY_LENGTH to queryLength,
                P_RESULT_COUNT to resultCount,
            ),
        )
    }

    fun channelPlayStarted(channelName: String, category: String) {
        analytics.logEvent(
            EVT_CHANNEL_PLAY_STARTED,
            bundleOf(
                P_CHANNEL_NAME to channelName.take(100),
                P_CHANNEL_CATEGORY to category.take(40),
                P_SOURCE to SOURCE_TECHJAIL,
            ),
        )
    }

    fun updateAccepted(fromVersion: String, toVersion: String) {
        analytics.logEvent(
            EVT_UPDATE_ACCEPTED,
            bundleOf(
                P_FROM_VERSION to fromVersion,
                P_TO_VERSION to toVersion,
            ),
        )
    }

    fun scraperDriftDetected(scraper: String, field: String) {
        analytics.logEvent(
            EVT_SCRAPER_DRIFT_DETECTED,
            bundleOf(
                P_SCRAPER to scraper,
                P_FIELD to field,
            ),
        )
    }

    private companion object {
        // Event names. Keep snake_case — Firebase recommends it.
        const val EVT_MEDIA_PLAY_STARTED = "media_play_started"
        const val EVT_MEDIA_PLAY_FAILED = "media_play_failed"
        const val EVT_SEARCH_PERFORMED = "search_performed"
        const val EVT_CHANNEL_PLAY_STARTED = "channel_play_started"
        const val EVT_UPDATE_ACCEPTED = "update_accepted"
        const val EVT_SCRAPER_DRIFT_DETECTED = "scraper_drift_detected"

        // Param keys. <=40 chars, snake_case.
        const val P_KIND = "kind"
        const val P_TITLE = "title"
        const val P_SOURCE = "source"
        const val P_REASON = "reason"
        const val P_QUERY_LENGTH = "query_length"
        const val P_RESULT_COUNT = "result_count"
        const val P_CHANNEL_NAME = "channel_name"
        const val P_CHANNEL_CATEGORY = "channel_category"
        const val P_FROM_VERSION = "from_version"
        const val P_TO_VERSION = "to_version"
        const val P_SCRAPER = "scraper"
        const val P_FIELD = "field"

        const val SOURCE_LOOKMOVIE = "lookmovie"
        const val SOURCE_TECHJAIL = "techjail"
    }
}
