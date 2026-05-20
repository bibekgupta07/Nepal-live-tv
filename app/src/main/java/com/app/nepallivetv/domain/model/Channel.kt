package com.app.nepallivetv.domain.model

/**
 * Pure domain entity. No serialization annotations, no Android imports —
 * the domain layer must not depend on data or framework concerns.
 */
data class Channel(
    val name: String,
    val encodedUrl: String,
    val logo: String? = null,
    val category: String = "All"
)
