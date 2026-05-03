package com.app.nepallivetv.domain.usecase

import com.app.nepallivetv.data.model.MatchDetail
import com.app.nepallivetv.domain.repository.ChannelRepository

class GetMatchDetailUseCase(private val repository: ChannelRepository) {
    suspend operator fun invoke(matchId: String): MatchDetail? {
        return repository.getMatchDetail(matchId)
    }
}