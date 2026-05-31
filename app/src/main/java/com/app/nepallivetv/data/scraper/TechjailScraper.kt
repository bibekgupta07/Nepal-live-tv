package com.app.nepallivetv.data.scraper

import com.app.nepallivetv.analytics.RemoteConfigService

/**
 * On-device resolver for techjail's per-channel m3u8 URLs. Backend used to
 * keep a 5-min cache; we don't — the tokens are short-lived and a single
 * channel select happens ~once every few minutes per user anyway.
 */
class TechjailScraper(
    private val http: ScraperHttpClient,
    private val remoteConfig: RemoteConfigService,
) {
    suspend fun getStreamUrl(channelId: String): String {
        val base = remoteConfig.techjailBaseUrl.trimEnd('/')
        val text = http.getText("$base/getlink.php?vv=1&CHID=$channelId") ?: return ""
        val trimmed = text.trim()
        return if (trimmed.startsWith("http") && ".m3u8" in trimmed.lowercase()) trimmed else ""
    }
}
