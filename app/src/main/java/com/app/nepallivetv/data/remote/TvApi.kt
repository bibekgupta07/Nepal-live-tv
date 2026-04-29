package com.app.nepallivetv.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Headers

interface TvApi {
    @GET("v9x9/")
    suspend fun getChannelsPage(): String

    @Headers(
        "Referer: http://tv.techjail.net/",
        "Origin: http://tv.techjail.net"
    )
    @GET("huritv9/getlink.php")
    suspend fun getStreamUrl(
        @Query("vv") vv: Int = 1,
        @Query("CHID") channelId: String
    ): String
}
