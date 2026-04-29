package com.app.nepallivetv.di

import com.app.nepallivetv.BuildConfig
import com.app.nepallivetv.data.remote.TvApi
import com.app.nepallivetv.data.repository.ChannelRepositoryImpl
import com.app.nepallivetv.domain.repository.ChannelRepository
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import com.app.nepallivetv.presentation.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

val appModule = module {

    single {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(TvApi::class.java)
    }

    single<ChannelRepository> {
        ChannelRepositoryImpl(get())
    }

    factory { GetChannelsUseCase(get()) }
    factory { GetStreamUrlUseCase(get()) }

    viewModel { MainViewModel(get(), get()) }
}
