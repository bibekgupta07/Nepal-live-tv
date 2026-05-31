package com.app.nepallivetv.data.remote

import com.app.nepallivetv.data.remote.dto.ChannelDto
import com.app.nepallivetv.data.remote.dto.HomeFeedDto
import com.app.nepallivetv.data.scraper.ScraperHttpClient
import kotlinx.serialization.json.Json

/**
 * Reads the pre-scraped JSON catalog committed by .github/workflows/scrape.yml.
 * Lives on `main` and is refreshed every 6h. App reads it raw from GitHub —
 * no backend involved.
 */
class CatalogApi(
    private val http: ScraperHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getHome(): HomeFeedDto? {
        val text = http.getText(HOME_URL) ?: return null
        return runCatching { json.decodeFromString<HomeFeedDto>(text) }.getOrNull()
    }

    suspend fun getChannels(): List<ChannelDto> {
        val text = http.getText(CHANNELS_URL) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ChannelDto>>(text)
        }.getOrNull() ?: emptyList()
    }

    private companion object {
        const val BASE =
            "https://raw.githubusercontent.com/bibekgupta07/Nepal-live-tv/main/data"
        const val HOME_URL = "$BASE/movies-home.json"
        const val CHANNELS_URL = "$BASE/channels.json"
    }
}
