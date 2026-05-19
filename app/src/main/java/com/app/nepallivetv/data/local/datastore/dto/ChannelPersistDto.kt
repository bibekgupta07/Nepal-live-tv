package com.app.nepallivetv.data.local.datastore.dto

import com.app.nepallivetv.domain.model.Channel
import kotlinx.serialization.Serializable

/**
 * Storage-format DTO used to round-trip favorites through DataStore.
 * Lives in the data layer so the domain [Channel] stays free of
 * serialization annotations.
 */
@Serializable
internal data class ChannelPersistDto(
    val name: String,
    val encodedUrl: String,
    val logo: String? = null,
    val category: String = "All"
) {
    fun toDomain(): Channel = Channel(name, encodedUrl, logo, category)

    companion object {
        fun fromDomain(c: Channel): ChannelPersistDto =
            ChannelPersistDto(c.name, c.encodedUrl, c.logo, c.category)
    }
}
