package com.app.nepallivetv.domain.repository

import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.data.model.Match
import com.app.nepallivetv.data.model.MatchDetail

interface ChannelRepository {
    suspend fun getChannels(): List<Channel>
    suspend fun getCricketMatches(): List<Match>
    suspend fun getMatchDetail(matchId: String): MatchDetail?
}
