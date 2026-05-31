package com.app.nepallivetv.data.remote.dto

import com.app.nepallivetv.domain.model.Episode
import com.app.nepallivetv.domain.model.HomeFeed
import com.app.nepallivetv.domain.model.HomeRow
import com.app.nepallivetv.domain.model.MediaDetail
import com.app.nepallivetv.domain.model.MediaItem
import com.app.nepallivetv.domain.model.MediaKind
import com.app.nepallivetv.domain.model.Season
import com.app.nepallivetv.domain.model.StreamSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Network DTOs mirroring the /api/movies endpoints. Kept separate from the
// domain models so the wire format can evolve (renamed fields, envelopes)
// without leaking into the rest of the app.

@Serializable
data class MediaItemDto(
    val id: String,
    val kind: String,
    val title: String,
    val year: Int? = null,
    val poster: String = "",
    val backdrop: String = "",
) {
    fun toDomain() = MediaItem(
        id = id,
        kind = if (kind == "show") MediaKind.SHOW else MediaKind.MOVIE,
        title = title,
        year = year,
        poster = poster,
        backdrop = backdrop,
    )
}

@Serializable
data class HomeRowDto(
    val title: String,
    val items: List<MediaItemDto>,
) {
    fun toDomain() = HomeRow(title = title, items = items.map { it.toDomain() })
}

@Serializable
data class HomeFeedDto(
    val hero: List<MediaItemDto>,
    val rows: List<HomeRowDto>,
) {
    fun toDomain() = HomeFeed(
        hero = hero.map { it.toDomain() },
        rows = rows.map { it.toDomain() },
    )
}

@Serializable
data class EpisodeDto(
    val idEpisode: Long,
    val season: Int,
    val number: Int,
    val title: String,
    val overview: String? = null,
    val still: String? = null,
) {
    fun toDomain() = Episode(
        idEpisode = idEpisode,
        season = season,
        number = number,
        title = title,
        overview = overview,
        still = still,
    )
}

@Serializable
data class SeasonDto(
    val number: Int,
    val episodes: List<EpisodeDto>,
) {
    fun toDomain() = Season(number = number, episodes = episodes.map { it.toDomain() })
}

@Serializable
data class MediaDetailDto(
    val id: String,
    val kind: String,
    val title: String,
    val year: Int? = null,
    val poster: String = "",
    val backdrop: String = "",
    val overview: String = "",
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val runtime: String? = null,
    val rating: Double? = null,
    val playerUrl: String,
    val upstreamId: String = "",
    val seasons: List<SeasonDto> = emptyList(),
) {
    fun toDomain() = MediaDetail(
        id = id,
        kind = if (kind == "show") MediaKind.SHOW else MediaKind.MOVIE,
        title = title,
        year = year,
        poster = poster,
        backdrop = backdrop,
        overview = overview,
        genres = genres,
        cast = cast,
        runtime = runtime,
        rating = rating,
        playerUrl = playerUrl,
        upstreamId = upstreamId,
        seasons = seasons.map { it.toDomain() },
    )
}

@Serializable
data class StreamSetDto(
    val p480: String = "",
    val p720: String = "",
    val p1080: String = "",
    val best: String = "",
    // Subtitles are ignored client-side for now; lookmovie ships dozens.
) {
    fun toDomain() = StreamSet(p480 = p480, p720 = p720, p1080 = p1080, best = best)
}
