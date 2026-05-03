package com.app.nepallivetv.di

import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import com.app.nepallivetv.data.repository.ChannelRepositoryImpl
import com.app.nepallivetv.domain.repository.ChannelRepository
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import com.app.nepallivetv.domain.usecase.GetCricketMatchesUseCase
import com.app.nepallivetv.domain.usecase.GetMatchDetailUseCase
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.presentation.viewmodel.AuthViewModel
import com.app.nepallivetv.presentation.viewmodel.MatchDetailViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { DatastorePreferences(androidContext()) }

    single<ChannelRepository> {
        ChannelRepositoryImpl(get(), androidContext())
    }

    factory { GetChannelsUseCase(get()) }
    factory { GetStreamUrlUseCase(get()) }
    factory { GetCricketMatchesUseCase(get()) }
    factory { GetMatchDetailUseCase(get()) }

    viewModel { SharedViewModel(get(), get(), get(), get()) }
    viewModel { MatchDetailViewModel(get()) }
    viewModel { AuthViewModel(get(), get()) }
}
