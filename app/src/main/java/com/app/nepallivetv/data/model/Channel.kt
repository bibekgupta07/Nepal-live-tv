package com.app.nepallivetv.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val name: String,
    val encodedUrl: String,
    val logo: String? = null,
    val category: String = "All"
)
