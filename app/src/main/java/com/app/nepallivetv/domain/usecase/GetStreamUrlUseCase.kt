package com.app.nepallivetv.domain.usecase

import com.app.nepallivetv.domain.repository.ChannelRepository

class GetStreamUrlUseCase(private val repository: ChannelRepository) {
    suspend operator fun invoke(channelId: String): String {
        return repository.getStreamUrl(channelId)
    }
}
