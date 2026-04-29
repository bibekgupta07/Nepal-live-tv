package com.app.nepallivetv.data.repository

import android.content.Context
import android.util.Log
import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class ChannelRepositoryImpl(
    private val context: Context
) : ChannelRepository {

    override suspend fun getChannels(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("channels.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val channels = mutableListOf<Channel>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                channels.add(
                    Channel(
                        name = jsonObject.getString("name"),
                        encodedUrl = jsonObject.getString("encodedUrl"),
                        logo = jsonObject.optString("logo")
                    )
                )
            }
            channels
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching channels", e)
            e.printStackTrace()
            emptyList()
        }
    }
}
