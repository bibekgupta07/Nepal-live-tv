package com.app.nepallivetv.data.repository

import android.util.Base64
import com.app.nepallivetv.data.remote.CatalogApi
import com.app.nepallivetv.data.scraper.TechjailScraper
import com.app.nepallivetv.domain.model.Channel
import com.app.nepallivetv.domain.repository.ChannelRepository

/**
 * v3: list of live channels comes from the JSON snapshot committed by the
 * Actions cron (CatalogApi → raw.gh). Per-channel m3u8 resolution happens on
 * the device via [TechjailScraper] at the moment the user taps a channel.
 */
class ChannelRepositoryImpl(
    private val catalog: CatalogApi,
    private val scraper: TechjailScraper,
) : ChannelRepository {

    override suspend fun getChannels(): List<Channel> =
        catalog.getChannels().map { it.toDomain() }

    override suspend fun getStreamUrl(encodedUrl: String): String {
        // Pure numeric IDs (techjail) need fresh tokens — resolve at click time.
        // Older base64-encoded entries (legacy snapshot data) decode locally.
        return if (encodedUrl.all { it.isDigit() }) {
            scraper.getStreamUrl(encodedUrl)
        } else {
            runCatching { String(Base64.decode(encodedUrl, Base64.DEFAULT)) }
                .getOrDefault("")
        }
    }
}
