package com.app.nepallivetv.data.scraper

import com.app.nepallivetv.analytics.RemoteConfigService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// Thin shared OkHttp wrapper for the on-device scrapers. Every call inherits
// the timeout from RemoteConfigService.scraperTimeoutMs so we can dial it from
// Firebase if lookmovie/techjail starts misbehaving in the wild.
class ScraperHttpClient(
    private val remoteConfig: RemoteConfigService,
) {
    @Volatile
    private var cached: Pair<Long, OkHttpClient>? = null

    private fun client(): OkHttpClient {
        val timeout = remoteConfig.scraperTimeoutMs
        cached?.let { (cachedTimeout, c) -> if (cachedTimeout == timeout) return c }
        val built = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .callTimeout(timeout, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .build()
        cached = timeout to built
        return built
    }

    suspend fun getText(url: String, headers: Map<String, String> = emptyMap()): String? =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(url)
                .header("User-Agent", UA)
                .header("Accept", ACCEPT)
                .header("Accept-Language", "en-US,en;q=0.9")
            headers.forEach { (k, v) -> builder.header(k, v) }
            runCatching {
                client().newCall(builder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body?.string()
                }
            }.getOrNull()
        }

    companion object {
        const val UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        const val ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    }
}
