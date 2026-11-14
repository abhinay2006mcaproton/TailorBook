package com.tailorbook

import android.app.Application
import com.tailorbook.core.ads.AdMobInitializer
import com.tailorbook.core.sync.worker.TailorSyncWorker
import timber.log.Timber

/**
 * Main Application class for TailorBook.
 * Initializes logging, offline databases, and background WorkManager synchronization.
 */
class TailorBookApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeLogging()
        TailorSyncWorker.schedulePeriodicSync(this)
        AdMobInitializer.initialize(this)
    }

    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("TailorBook application initialized in DEBUG mode. MinSdk=26, TargetSdk=34.")
        }
    }
}
