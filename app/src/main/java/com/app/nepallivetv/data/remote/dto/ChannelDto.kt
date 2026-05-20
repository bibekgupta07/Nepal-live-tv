package com.app.nepallivetv.data.remote.dto

import com.app.nepallivetv.domain.model.Channel
import kotlinx.serialization.Serializable

/**
 * Network DTO mirroring the backend's /api/channels response. Kept separate
 * from the domain [Channel] so wire-format changes (renamed fields, added
 * envelopes) don't leak past the data layer.
 */
@Serializable
data class ChannelDto(
    val name: String,
    val encodedUrl: String,
    val logo: String? = null,
    val category: String = "All"
) {
    fun toDomain(): Channel = Channel(
        name = name,
        encodedUrl = encodedUrl,
        logo = logo,
        category = category
    )
}
