package com.app.nepallivetv.data.repository

import android.content.Context
import android.util.Log
import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Implementation of [ChannelRepository] that provides data from local JSON assets.
 * In a real-world/market-level application, this could easily be swapped out to fetch from a Retrofit API.
 */
class ChannelRepositoryImpl(
    private val context: Context
) : ChannelRepository {

    /**
     * Fetches the list of channels from the local `channels.json` file in the assets folder.
     * Runs on the IO dispatcher to ensure it doesn't block the main UI thread during file reading/parsing.
     */
    override suspend fun getChannels(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            // 1. Read the JSON file from the assets folder into a String
            val jsonString = context.assets.open("channels.json").bufferedReader().use { it.readText() }
            
            // 2. Parse the String into a JSON Array
            val jsonArray = JSONArray(jsonString)
            val channels = mutableListOf<Channel>()
            
            // 3. Iterate through the array and map JSON objects to our Channel Kotlin data class
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                channels.add(
                    Channel(
                        name = jsonObject.getString("name"),
                        encodedUrl = jsonObject.getString("encodedUrl"), // This is stored as Base64 for security
                        logo = jsonObject.optString("logo")
                    )
                )
            }
            
            // Return the populated list
            channels
        } catch (e: Exception) {
            // Log any parsing or file reading errors
            Log.e("ChannelRepository", "Error fetching channels from assets", e)
            e.printStackTrace()
            // Return empty list as a safe fallback
            emptyList()
        }
    }
}
