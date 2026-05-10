package com.app.nepallivetv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val id: Int,
    val title: String,
    val date: String? = null,
    val duration: Int = 0,
    val size_bytes: Long = 0,
    val stream_url: String,
    val type: String = "movie",
    val name: String = "Unknown",
    val season: String? = null,
    val episode: String? = null,
    val quality: String = "Unknown",
    val thumbnail_url: String? = null
)

@Serializable
data class MovieResponse(
    val movies: List<Movie>
)
