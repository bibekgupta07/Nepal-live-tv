package com.app.nepallivetv.domain.usecase

import android.util.Base64
import com.app.nepallivetv.data.remote.LiveTvApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetStreamUrlUseCase(
    private val api: LiveTvApi
) {
    suspend operator fun invoke(encodedUrl: String): String = withContext(Dispatchers.IO) {
        try {
            // Check if it is a pure numeric ID (techjail ID)
            if (encodedUrl.all { it.isDigit() }) {
                val response = api.getStreamUrl(encodedUrl)
                return@withContext response.stream_url
            }
            
            // Fallback for older Base64 encoded URLs if any remain
            val decodedBytes = Base64.decode(encodedUrl, Base64.DEFAULT)
            String(decodedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
