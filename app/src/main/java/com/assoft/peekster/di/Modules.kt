package com.assoft.peekster.di

import com.assoft.peekster.activity.MainViewModel
import com.assoft.peekster.activity.MediaViewModel
import com.assoft.peekster.domain.usecase.*
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module

fun injectFeature() = loadFeature

/** Load list of modules */
private val loadFeature by lazy {
    loadKoinModules(
        listOf(
            viewModelModule,
            useCaseModule
        )
    )
}

val viewModelModule: Module = module {
    /** View model for [MediaViewModel] */
    viewModel {
        MediaViewModel(
            categoryRepository = get(),
            moviesRepository = get(),
            tvShowRepository = get(),
            parentalControlActionUseCase = get(),
            parentalControlUseCase = get()
        )
    }
    viewModel {
        MainViewModel(
            introductionShownActionUseCase = get(),
            introductionShownUseCase = get(),
            getDeviceNameUseCase = get(),
            saveDeviceNameActionUseCase = get(),
            serverRepository = get()
        )
    }
}

/**
 * A factory component declaration is a definition that will gives you a new
 * instance each time you ask for this definition (this instance is not retrained
 * by Koin container, as it won’t inject this instance in other definitions later).
 */
val useCaseModule: Module = module {
    factory {
        ParentalControlActionUseCase(
            preferenceStorage = get()
        )
    }
    factory {
        ParentControlUseCase(
            preferenceStorage = get()
        )
    }

    factory {
        IntroductionShownActionUseCase(
            preferenceStorage = get()
        )
    }
    factory {
        IntroductionShownUseCase(
            preferenceStorage = get()
        )
    }
    factory {
        GetActiveServerUseCase(
            preferenceStorage = get()
        )
    }
    factory {
        SetActiveServerActionUseCase(
            preferenceStorage = get()
        )
    }
    factory {
        GetDeviceNameUseCase(
            preferenceStorage = get()
        )
    }
    factory {
        SaveDeviceNameActionUseCase(
            preferenceStorage = get()
        )
    }
}