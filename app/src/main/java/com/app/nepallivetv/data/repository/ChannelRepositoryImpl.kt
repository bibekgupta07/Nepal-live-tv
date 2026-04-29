package com.app.nepallivetv.data.repository

import android.util.Log
import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.data.remote.TvApi
import com.app.nepallivetv.domain.repository.ChannelRepository
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChannelRepositoryImpl(
    private val api: TvApi
) : ChannelRepository {

    override suspend fun getChannels(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val html = api.getChannelsPage()
            Log.d("ChannelRepository", "Fetched HTML length: ${html.length}")
            val document = Jsoup.parse(html)
            val channelElements = document.select("li.chindividual a.channels")
            Log.d("ChannelRepository", "Found ${channelElements.size} channels")
            
            channelElements.mapNotNull { element ->
                val id = element.attr("data-id")
                val name = element.attr("data-name")
                val imgElement = element.selectFirst("img")
                val imageUrl = imgElement?.attr("src") ?: ""
                
                if (id.isNotEmpty() && name.isNotEmpty()) {
                    Channel(id = id, name = name, imageUrl = imageUrl)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching channels", e)
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getStreamUrl(channelId: String): String = withContext(Dispatchers.IO) {
        try {
            val url = api.getStreamUrl(channelId = channelId).trim()
            Log.d("ChannelRepository", "Fetched Stream URL for $channelId: $url")
            // Clean up the URL if it returns something with whitespace or extra tags
            return@withContext url.replace("\"", "").replace("'", "").trim()
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching stream url", e)
            e.printStackTrace()
            ""
        }
    }
}
