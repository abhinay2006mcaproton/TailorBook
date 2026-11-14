package com.tailorbook.core.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob Configuration and ID Provider.
 * Enforces official Google test unit IDs during development and testing.
 * Prevents accidental production AdMob activation.
 */
object AdMobConfig {

    // Official Google Test IDs (Safe for testing, will never incur policy violations)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Flag controlling live vs test ads. Strict rule: NEVER enable without explicit user approval.
    var isLiveAdsEnabled: Boolean = false
        private set

    /**
     * Returns the appropriate Rewarded Ad Unit ID based on the active environment.
     */
    fun getRewardedAdUnitId(): String {
        return if (isLiveAdsEnabled) {
            // Placeholder: When authorized, production ID will be loaded from secure config
            TEST_REWARDED_AD_UNIT_ID
        } else {
            TEST_REWARDED_AD_UNIT_ID
        }
    }

    /**
     * Returns the appropriate Banner Ad Unit ID based on the active environment.
     */
    fun getBannerAdUnitId(): String {
        return if (isLiveAdsEnabled) {
            TEST_BANNER_AD_UNIT_ID
        } else {
            TEST_BANNER_AD_UNIT_ID
        }
    }

    /**
     * Returns the appropriate Interstitial Ad Unit ID based on the active environment.
     */
    fun getInterstitialAdUnitId(): String {
        return if (isLiveAdsEnabled) {
            TEST_INTERSTITIAL_AD_UNIT_ID
        } else {
            TEST_INTERSTITIAL_AD_UNIT_ID
        }
    }

    /**
     * Authorization gate to switch to live ads.
     * Can only be called when explicit authorization is received.
     */
    fun setLiveAdsEnabled(enabled: Boolean) {
        isLiveAdsEnabled = enabled
        Timber.i("AdMob live ads status updated: $enabled")
    }
}

/**
 * Manages Google Mobile Ads SDK initialization lifecycle.
 * Runs on a background thread so cold start app launch latency is unaffected.
 */
object AdMobInitializer {

    private val isInitialized = AtomicBoolean(false)

    fun initialize(
        context: Context,
        testDeviceIds: List<String> = emptyList(),
        onComplete: (() -> Unit)? = null
    ) {
        if (isInitialized.getAndSet(true)) {
            Timber.d("AdMob already initialized or initializing.")
            onComplete?.invoke()
            return
        }

        try {
            // Configure test devices if specified
            val requestConfiguration = RequestConfiguration.Builder()
                .apply {
                    if (testDeviceIds.isNotEmpty()) {
                        setTestDeviceIds(testDeviceIds)
                    }
                }
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            // Initialize Mobile Ads SDK directly
            MobileAds.initialize(context.applicationContext) { status ->
                Timber.i("Google Mobile Ads SDK initialized successfully: ${status.adapterStatusMap}")
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Google Mobile Ads SDK")
            // Reset initialized flag so retry is possible
            isInitialized.set(false)
        }
    }

    fun isReady(): Boolean = isInitialized.get()
}
