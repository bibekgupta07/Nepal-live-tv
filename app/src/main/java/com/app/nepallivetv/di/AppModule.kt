package com.app.nepallivetv.di

import com.app.nepallivetv.data.repository.ChannelRepositoryImpl
import com.app.nepallivetv.domain.repository.ChannelRepository
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import com.app.nepallivetv.presentation.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single<ChannelRepository> {
        ChannelRepositoryImpl(androidContext())
    }

    factory { GetChannelsUseCase(get()) }
    factory { GetStreamUrlUseCase() }

    viewModel { MainViewModel(get(), get()) }
}
