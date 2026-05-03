package com.app.nepallivetv.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Match(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val format: String = "",
    val state: String = "",
    @SerialName("status_text") val statusText: String = "",
    @SerialName("start_time") val startTime: String = "",
    val team1: Team? = null,
    val team2: Team? = null
)

@Serializable
data class Team(
    val name: String = "",
    val abbr: String = "",
    val logo: String? = null,
    val score: String? = null,
    val overs: String? = null
)