package com.app.nepallivetv.domain.repository

import com.app.nepallivetv.domain.model.Channel

interface ChannelRepository {
    suspend fun getChannels(): List<Channel>
    suspend fun getStreamUrl(encodedUrl: String): String
}
