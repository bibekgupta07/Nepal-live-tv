package com.app.nepallivetv.data.remote

import com.app.nepallivetv.data.model.RegisterRequest
import com.app.nepallivetv.data.model.RegisterResponse
import com.app.nepallivetv.data.model.TokenResponse
import com.app.nepallivetv.data.remote.dto.ChannelDto
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class StreamResponse(
    val stream_url: String
)

interface LiveTvApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") loginId: String,
        @Field("password") pass: String
    ): TokenResponse

    @GET("api/channels")
    suspend fun getChannels(): List<ChannelDto>

    @GET("api/stream/{channel_id}")
    suspend fun getStreamUrl(@Path("channel_id") channelId: String): StreamResponse
}
