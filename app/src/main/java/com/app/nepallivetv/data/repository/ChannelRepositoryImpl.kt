package com.app.nepallivetv.data.repository

import android.content.Context
import android.util.Log
import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.data.model.Match
import com.app.nepallivetv.data.model.MatchDetail
import com.app.nepallivetv.data.remote.LiveTvApi
import com.app.nepallivetv.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class ChannelRepositoryImpl(
    private val api: LiveTvApi,
    private val context: Context
) : ChannelRepository {

    override suspend fun getChannels(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            api.getChannels()
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching channels from backend", e)
            e.printStackTrace()
            
            // Fallback to local JSON if backend is offline
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
                            logo = jsonObject.optString("logo"),
                            category = jsonObject.optString("category", "All")
                        )
                    )
                }
                channels
            } catch (fallbackEx: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getCricketMatches(): List<Match> = withContext(Dispatchers.IO) {
        try {
            api.getCricketMatches()
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching cricket matches", e)
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getMatchDetail(matchId: String): MatchDetail? = withContext(Dispatchers.IO) {
        try {
            api.getMatchDetail(matchId)
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error fetching match details", e)
            e.printStackTrace()
            null
        }
    }
}
