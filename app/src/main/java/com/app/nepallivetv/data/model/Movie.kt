package com.app.nepallivetv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val id: Int,
    val title: String,
    val date: String? = null,
    val duration: Int = 0,
    val size_bytes: Long = 0,
    val stream_url: String
)

@Serializable
data class MovieResponse(
    val movies: List<Movie>
)
