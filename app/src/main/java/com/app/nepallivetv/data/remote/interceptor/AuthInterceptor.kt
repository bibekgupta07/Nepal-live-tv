package com.app.nepallivetv.data.remote.interceptor

import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * An industrial-standard Interceptor for OkHttp.
 * This intercepts every outgoing API request before it hits the network.
 * You can easily append headers here (like User-Agent, Accept, or Bearer Tokens).
 */
class AuthInterceptor(
    private val datastorePreferences: DatastorePreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // Don't force Content-Type here — Retrofit sets the right one per call
        // (application/json for @Body, application/x-www-form-urlencoded for
        // @FormUrlEncoded). Overriding it here broke the login endpoint, which
        // uses OAuth2PasswordRequestForm and rejects JSON content-type.
        requestBuilder.addHeader("Accept", "application/json")

        // Dynamic Auth Token
        // OkHttp interceptors run on a background worker thread.
        // It is completely safe and standard practice to use `runBlocking` here to read from DataStore
        // synchronously so the network request waits for the token to be fetched before proceeding.
        
        val token = runBlocking { datastorePreferences.getToken() }
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)
        
        // Auto-logout on 401 Unauthorized
        if (response.code == 401) {
            runBlocking {
                datastorePreferences.clearAuthData()
            }
        }
        
        return response
    }
}
