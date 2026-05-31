package com.app.nepallivetv.domain.model

/**
 * Catalog entities shared by Movies and TV Shows. The same shape covers both
 * — the [kind] field switches behavior on the UI side (e.g. show a "Watch
 * Episodes" CTA for shows, "Play Movie" for movies).
 */

enum class MediaKind { MOVIE, SHOW }

data class MediaItem(
    val id: String,
    val kind: MediaKind,
    val title: String,
    val year: Int?,
    val poster: String,
    val backdrop: String,
)

data class HomeRow(
    val title: String,
    val items: List<MediaItem>,
)

data class HomeFeed(
    val hero: List<MediaItem>,
    val rows: List<HomeRow>,
)

data class Episode(
    val idEpisode: Long,
    val season: Int,
    val number: Int,
    val title: String,
    val overview: String?,
    val still: String?,
)

data class Season(
    val number: Int,
    val episodes: List<Episode>,
)

data class MediaDetail(
    val id: String,
    val kind: MediaKind,
    val title: String,
    val year: Int?,
    val poster: String,
    val backdrop: String,
    val overview: String,
    val genres: List<String>,
    val cast: List<String>,
    val runtime: String?,
    val rating: Double?,
    val playerUrl: String,
    val upstreamId: String,
    val seasons: List<Season>,
)

/** Quality-tagged HLS URLs returned by the backend. */
data class StreamSet(
    val p480: String,
    val p720: String,
    val p1080: String,
    val best: String,
) {
    val hasAny: Boolean get() = best.isNotBlank()
}
