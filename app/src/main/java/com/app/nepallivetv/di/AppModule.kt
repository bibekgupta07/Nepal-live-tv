package com.app.nepallivetv.di

import com.app.nepallivetv.data.repository.ChannelRepositoryImpl
import com.app.nepallivetv.domain.repository.ChannelRepository
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import com.app.nepallivetv.presentation.screens.home.HomeViewModel
import com.app.nepallivetv.data.local.datastore.ThemePreferences
import com.app.nepallivetv.presentation.ThemeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { ThemePreferences(androidContext()) }

    single<ChannelRepository> {
        ChannelRepositoryImpl(androidContext())
    }

    factory { GetChannelsUseCase(get()) }
    factory { GetStreamUrlUseCase() }

    viewModel { HomeViewModel(get(), get()) }
    viewModel { ThemeViewModel(get()) }
}
