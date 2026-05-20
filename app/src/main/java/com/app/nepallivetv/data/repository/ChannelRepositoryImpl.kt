package com.app.nepallivetv.data.repository

import android.util.Base64
import android.util.Log
import com.app.nepallivetv.data.remote.LiveTvApi
import com.app.nepallivetv.domain.model.Channel
import com.app.nepallivetv.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChannelRepositoryImpl(
    private val api: LiveTvApi
) : ChannelRepository {

    override suspend fun getChannels(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            api.getChannels().map { it.toDomain() }
        } catch (e: Exception) {
            // No offline fallback — a stale bundled snapshot would surface
            // dead channels that can't actually play, which is worse UX than
            // an honest empty state. The screen already shows "No channels
            // found." when this returns empty.
            Log.e("ChannelRepository", "Failed to fetch channels from backend", e)
            emptyList()
        }
    }

    override suspend fun getStreamUrl(encodedUrl: String): String = withContext(Dispatchers.IO) {
        try {
            // Pure numeric IDs (techjail IDs) round-trip through the backend so
            // we always get a fresh tokenized URL. Older base64-encoded entries
            // (legacy data) decode locally.
            if (encodedUrl.all { it.isDigit() }) {
                return@withContext api.getStreamUrl(encodedUrl).stream_url
            }
            String(Base64.decode(encodedUrl, Base64.DEFAULT))
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching stream url", e)
            ""
        }
    }
}
