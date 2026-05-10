package com.app.nepallivetv.updater

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("assets") val assets: List<GitHubAsset>,
    @SerialName("body") val body: String? = null
)

@Serializable
data class GitHubAsset(
    @SerialName("browser_download_url") val downloadUrl: String
)
