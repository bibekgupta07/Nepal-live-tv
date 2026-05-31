package com.app.nepallivetv.di

import com.app.nepallivetv.data.local.datastore.DatastorePreferences
import com.app.nepallivetv.data.remote.CatalogApi
import com.app.nepallivetv.data.repository.ChannelRepositoryImpl
import com.app.nepallivetv.data.repository.MediaRepositoryImpl
import com.app.nepallivetv.data.scraper.LookmovieScraper
import com.app.nepallivetv.data.scraper.ScraperHttpClient
import com.app.nepallivetv.data.scraper.TechjailScraper
import com.app.nepallivetv.domain.repository.ChannelRepository
import com.app.nepallivetv.domain.repository.MediaRepository
import com.app.nepallivetv.domain.usecase.GetChannelsUseCase
import com.app.nepallivetv.domain.usecase.GetStreamUrlUseCase
import com.app.nepallivetv.presentation.screens.movies.MoviePlayerViewModel
import com.app.nepallivetv.presentation.viewmodel.MoviesViewModel
import com.app.nepallivetv.presentation.viewmodel.SharedViewModel
import com.app.nepallivetv.updater.UpdateManager
import com.app.nepallivetv.updater.UpdateViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { DatastorePreferences(androidContext()) }

    // v3 data layer: catalog comes from raw.gh JSON (CatalogApi); click-time
    // scraping runs on-device. No backend in the picture.
    single { ScraperHttpClient(get()) }
    single { CatalogApi(get()) }
    single { LookmovieScraper(get(), get(), get()) }
    single { TechjailScraper(get(), get()) }

    single<ChannelRepository> { ChannelRepositoryImpl(get(), get()) }
    single<MediaRepository> { MediaRepositoryImpl(get(), get()) }

    single { UpdateManager(androidContext()) }

    factory { GetChannelsUseCase(get()) }
    factory { GetStreamUrlUseCase(get<ChannelRepository>()) }

    viewModel { SharedViewModel(get(), get(), get(), get(), get()) }
    viewModel { UpdateViewModel(get(), get()) }
    viewModel { MoviesViewModel(get(), get()) }
    viewModel { MoviePlayerViewModel(get(), get()) }
}
