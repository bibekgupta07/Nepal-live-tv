package com.app.nepallivetv.domain.usecase

import com.app.nepallivetv.domain.repository.ChannelRepository

class GetStreamUrlUseCase(
    private val repository: ChannelRepository
) {
    suspend operator fun invoke(encodedUrl: String): String =
        repository.getStreamUrl(encodedUrl)
}
