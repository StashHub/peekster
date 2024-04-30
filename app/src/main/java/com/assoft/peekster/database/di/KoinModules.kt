package com.assoft.peekster.database.di

import com.assoft.peekster.database.PeeksterDatabase
import com.assoft.peekster.repository.CategoryRepository
import com.assoft.peekster.repository.MoviesRepository
import com.assoft.peekster.domain.prefs.PreferenceStorage
import com.assoft.peekster.domain.prefs.SharedPreferenceStorage
import com.assoft.peekster.repository.ServerRepository
import com.assoft.peekster.repository.TvShowRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val dbModule = module {
    single { PeeksterDatabase.getInstance(
        context = get()
    )}
    factory { get<PeeksterDatabase>().categoryDao() }
    factory { get<PeeksterDatabase>().movieDao() }
    factory { get<PeeksterDatabase>().tvShowDao() }
    factory { get<PeeksterDatabase>().httpServerDao() }
}

val sharedModule: Module = module {
    /** Factory instance of [PreferenceStorage] instance */
    single<PreferenceStorage> {
        SharedPreferenceStorage(
            context = get()
        )
    }
}

val repositoryModule = module {
    single { CategoryRepository(get()) }
    single { MoviesRepository(get()) }
    single { TvShowRepository(get()) }
    single { ServerRepository(get()) }
}