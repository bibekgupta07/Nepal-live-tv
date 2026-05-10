package com.app.nepallivetv.di

import com.app.nepallivetv.BuildConfig
import com.app.nepallivetv.data.remote.LiveTvApi
import com.app.nepallivetv.data.remote.interceptor.AuthInterceptor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {

    // 1. Provide the Interceptor
    single { AuthInterceptor(get()) }

    // 2. Provide OkHttpClient
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.ENABLE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        OkHttpClient.Builder()
            .addInterceptor(get<AuthInterceptor>())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 3. Provide Retrofit
    single {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    // 4. Provide the LiveTvApi service
    single { 
        get<Retrofit>().create(LiveTvApi::class.java) 
    }
}
