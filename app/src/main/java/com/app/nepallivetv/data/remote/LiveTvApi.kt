package com.app.nepallivetv.data.remote

import com.app.nepallivetv.data.model.Channel
import com.app.nepallivetv.data.model.Match
import com.app.nepallivetv.data.model.MatchDetail
import com.app.nepallivetv.data.model.LoginRequest
import com.app.nepallivetv.data.model.RegisterRequest
import com.app.nepallivetv.data.model.RegisterResponse
import com.app.nepallivetv.data.model.TokenResponse
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path

@Serializable
data class StreamResponse(
    val stream_url: String
)

interface LiveTvApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @GET("api/channels")
    suspend fun getChannels(): List<Channel>

    @GET("api/stream/{channel_id}")
    suspend fun getStreamUrl(@Path("channel_id") channelId: String): StreamResponse

    @GET("api/cricket")
    suspend fun getCricketMatches(): List<Match>

    @GET("api/cricket/{match_id}")
    suspend fun getMatchDetail(@Path("match_id") matchId: String): MatchDetail
}
