package com.app.nepallivetv.domain.usecase

import android.util.Base64

class GetStreamUrlUseCase {
    operator fun invoke(encodedUrl: String): String {
        return try {
            val decodedBytes = Base64.decode(encodedUrl, Base64.DEFAULT)
            String(decodedBytes)
        } catch (e: Exception) {
            ""
        }
    }
}
