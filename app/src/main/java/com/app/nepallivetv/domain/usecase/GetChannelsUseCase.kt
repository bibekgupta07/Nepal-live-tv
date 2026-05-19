package com.app.nepallivetv.domain.usecase

import com.app.nepallivetv.domain.model.Channel
import com.app.nepallivetv.domain.repository.ChannelRepository

class GetChannelsUseCase(private val repository: ChannelRepository) {
    suspend operator fun invoke(): List<Channel> = repository.getChannels()
}
