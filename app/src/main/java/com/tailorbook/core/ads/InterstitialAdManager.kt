package com.tailorbook.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Interface contract for managing Interstitial Video Ad operations.
 * Allows clean mocking for automated unit and UI testing.
 */
interface InterstitialAdManager {
    val isAdLoaded: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>

    /** Preloads an interstitial ad into memory. */
    fun preloadAd(context: Context)

    /**
     * Shows the interstitial ad if eligible (loaded, cooldown passed, and not premium).
     * Invokes [onAdDismissed] when completed or if skipped.
     */
    fun showAdIfEligible(
        activity: Activity,
        isPremium: Boolean,
        onAdDismissed: () -> Unit
    )
}

/**
 * Production implementation of [InterstitialAdManager] using Google Mobile Ads SDK.
 * Applies a strict 15-minute frequency cooldown and completely suppresses ads for Premium subscribers.
 */
class AdMobInterstitialAdManager(
    private val adUnitIdProvider: () -> String = { AdMobConfig.getInterstitialAdUnitId() },
    private val cooldownMillis: Long = 15 * 60 * 1000L // 15 minutes minimum interval
) : InterstitialAdManager {

    private val _isAdLoaded = MutableStateFlow(false)
    override val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentInterstitialAd: InterstitialAd? = null
    private var lastAdShownTimestamp: Long = 0L

    override fun preloadAd(context: Context) {
        if (_isAdLoaded.value || _isLoading.value) {
            Timber.d("Interstitial ad already loaded or loading.")
            return
        }

        _isLoading.value = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = adUnitIdProvider()

        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    currentInterstitialAd = ad
                    _isAdLoaded.value = true
                    _isLoading.value = false
                    Timber.i("Interstitial ad loaded successfully: $adUnitId")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    currentInterstitialAd = null
                    _isAdLoaded.value = false
                    _isLoading.value = false
                    Timber.w("Interstitial ad failed to load: ${error.message} (code: ${error.code})")
                }
            }
        )
    }

    override fun showAdIfEligible(
        activity: Activity,
        isPremium: Boolean,
        onAdDismissed: () -> Unit
    ) {
        if (isPremium) {
            Timber.d("User is Premium: Skipping interstitial ad.")
            onAdDismissed()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdShownTimestamp < cooldownMillis) {
            Timber.d("Interstitial cooldown active (%d sec remaining). Skipping ad.",
                (cooldownMillis - (currentTime - lastAdShownTimestamp)) / 1000)
            onAdDismissed()
            return
        }

        val ad = currentInterstitialAd
        if (ad == null || !_isAdLoaded.value) {
            Timber.d("Interstitial ad not ready. Skipping.")
            preloadAd(activity)
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                currentInterstitialAd = null
                _isAdLoaded.value = false
                lastAdShownTimestamp = System.currentTimeMillis()
                preloadAd(activity)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Timber.w("Failed to show interstitial ad: ${adError.message}")
                currentInterstitialAd = null
                _isAdLoaded.value = false
                preloadAd(activity)
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Timber.i("Interstitial ad presented to user.")
            }
        }

        ad.show(activity)
    }
}
