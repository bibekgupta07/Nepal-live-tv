package com.app.nepallivetv.domain.repository

import com.app.nepallivetv.data.model.Channel

interface ChannelRepository {
    suspend fun getChannels(): List<Channel>
    suspend fun getStreamUrl(channelId: String): String
}
