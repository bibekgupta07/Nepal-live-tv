package com.app.nepallivetv.data.repository

import com.app.nepallivetv.data.remote.CatalogApi
import com.app.nepallivetv.data.scraper.LookmovieScraper
import com.app.nepallivetv.domain.model.HomeFeed
import com.app.nepallivetv.domain.model.MediaDetail
import com.app.nepallivetv.domain.model.MediaItem
import com.app.nepallivetv.domain.model.MediaKind
import com.app.nepallivetv.domain.model.StreamSet
import com.app.nepallivetv.domain.repository.MediaRepository

/**
 * v3: home feed comes from a JSON snapshot committed by the GitHub Actions
 * cron (CatalogApi → raw.gh). Detail/search/stream are scraped on-device at
 * click time via [LookmovieScraper] so we don't have to keep a backend alive
 * just to forward HTTP calls.
 */
class MediaRepositoryImpl(
    private val catalog: CatalogApi,
    private val scraper: LookmovieScraper,
) : MediaRepository {

    override suspend fun getHome(): HomeFeed? = catalog.getHome()?.toDomain()

    override suspend fun getDetail(kind: MediaKind, id: String): MediaDetail? =
        scraper.getDetail(kind, id)

    override suspend fun search(query: String): List<MediaItem> = scraper.search(query)

    override suspend fun getMovieStream(slug: String): StreamSet? = scraper.getMovieStream(slug)

    override suspend fun getEpisodeStream(slug: String, idEpisode: Long): StreamSet? =
        scraper.getEpisodeStream(slug, idEpisode)
}
