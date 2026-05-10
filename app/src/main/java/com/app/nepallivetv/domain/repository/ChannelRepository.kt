package com.app.nepallivetv.domain.repository

import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.data.model.Movie

interface ChannelRepository {
    suspend fun getChannels(): List<Channel>
    suspend fun getMovies(limit: Int, offset: Int): List<Movie>
    suspend fun searchMovies(query: String, limit: Int, offset: Int): List<Movie>
}
