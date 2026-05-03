package com.app.nepallivetv.domain.usecase

import com.app.nepallivetv.data.model.Match
import com.app.nepallivetv.domain.repository.ChannelRepository

class GetCricketMatchesUseCase(private val repository: ChannelRepository) {
    suspend operator fun invoke(): List<Match> {
        return repository.getCricketMatches()
    }
}