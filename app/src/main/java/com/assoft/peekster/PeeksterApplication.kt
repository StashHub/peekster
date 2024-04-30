package com.assoft.peekster

import android.app.Application
import com.assoft.peekster.database.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

/**
 * Initialization of libraries.
 */
class PeeksterApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        /** Starts Koin Application */
        startKoin {
            /** Declare used Android context */
            /** Declare used Android context */
            androidContext(this@PeeksterApplication)

            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }

            /** Use Android logger - Level.INFO by default */

            /** Use Android logger - Level.INFO by default */
            androidLogger(Level.DEBUG)

            /** Inject list of modules for usage */

            /** Inject list of modules for usage */
            modules(listOf(sharedModule, dbModule, repositoryModule))
        }
    }
}