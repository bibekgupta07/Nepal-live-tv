package com.app.nepallivetv.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class MatchDetail(
    val id: String = "",
    val title: String = "",
    val state: String = "",
    @SerialName("status_text") val statusText: String = "",
    val scorecards: List<Scorecard> = emptyList()
)

@Serializable
data class Scorecard(
    val team: String = "",
    val score: String = "",
    val overs: String = "",
    val batsmen: List<Batsman> = emptyList(),
    val bowlers: List<Bowler> = emptyList()
)

@Serializable
data class Batsman(
    val name: String = "",
    val runs: Int? = null,
    val balls: Int? = null,
    val fours: Int? = null,
    val sixes: Int? = null,
    @SerialName("strike_rate") val strikeRate: Double? = null,
    @SerialName("is_out") val isOut: Boolean = false,
    val dismissal: Dismissal? = null
)

@Serializable
data class Dismissal(
    val short: String = "",
    val long: String = "",
    val commentary: String = "",
    val fielderText: String? = null,
    val bowlerText: String? = null
)

@Serializable
data class Bowler(
    val name: String = "",
    val overs: Double? = null,
    val maidens: Int? = null,
    val runs: Int? = null,
    val wickets: Int? = null,
    val economy: Double? = null
)