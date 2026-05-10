package com.app.nepallivetv.domain.usecase

import com.app.nepallivetv.data.model.Movie
import com.app.nepallivetv.domain.repository.ChannelRepository

class SearchMoviesUseCase(private val repository: ChannelRepository) {
    suspend operator fun invoke(query: String, limit: Int = 20, offset: Int = 0): List<Movie> {
        return repository.searchMovies(query, limit, offset)
    }
}
