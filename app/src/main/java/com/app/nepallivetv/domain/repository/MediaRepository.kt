package com.app.nepallivetv.domain.repository

import com.app.nepallivetv.domain.model.HomeFeed
import com.app.nepallivetv.domain.model.MediaDetail
import com.app.nepallivetv.domain.model.MediaItem
import com.app.nepallivetv.domain.model.MediaKind
import com.app.nepallivetv.domain.model.StreamSet

interface MediaRepository {
    suspend fun getHome(): HomeFeed?
    suspend fun getDetail(kind: MediaKind, id: String): MediaDetail?
    suspend fun search(query: String): List<MediaItem>
    suspend fun getMovieStream(slug: String): StreamSet?
    suspend fun getEpisodeStream(slug: String, idEpisode: Long): StreamSet?
}
