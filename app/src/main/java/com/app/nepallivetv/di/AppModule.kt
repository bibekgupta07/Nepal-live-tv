package com.app.nepallivetv.di

import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import com.app.nepallivetv.data.repository.ChannelRepositoryImpl
import com.app.nepallivetv.domain.repository.ChannelRepository
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.presentation.viewmodel.AuthViewModel
import com.app.nepallivetv.updater.UpdateManager
import com.app.nepallivetv.updater.UpdateViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { DatastorePreferences(androidContext()) }

    single<ChannelRepository> {
        ChannelRepositoryImpl(get())
    }

    single { UpdateManager(androidContext()) }

    factory { GetChannelsUseCase(get()) }
    // Use case now depends on the domain ChannelRepository, not LiveTvApi directly.
    factory { GetStreamUrlUseCase(get<ChannelRepository>()) }

    viewModel { SharedViewModel(get(), get(), get()) }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { UpdateViewModel(get()) }
}
