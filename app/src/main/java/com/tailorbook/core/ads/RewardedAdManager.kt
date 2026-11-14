package com.tailorbook.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Interface contract for managing Rewarded Video Ad operations.
 * Allows clean mocking for automated unit and UI testing.
 */
interface RewardedAdManager {
    val isAdLoaded: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>

    /** Preloads an ad into memory so it is immediately available when requested. */
    fun preloadAd(context: Context)

    /**
     * Displays the loaded rewarded ad.
     * Guaranteed to invoke [onUserEarnedReward] ONLY when the official Google reward callback triggers.
     * Invokes [onAdFailed] if the ad is closed early, fails to render, or is not available.
     */
    fun showAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdFailed: (errorMessage: String) -> Unit
    )
}

/**
 * Production implementation of [RewardedAdManager] using Google Mobile Ads SDK.
 * Handles ad lifecycle, single-use reward verification, and automatic preloading.
 */
class AdMobRewardedAdManager(
    private val adUnitIdProvider: () -> String = { AdMobConfig.getRewardedAdUnitId() }
) : RewardedAdManager {

    private val _isAdLoaded = MutableStateFlow(false)
    override val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentRewardedAd: RewardedAd? = null

    override fun preloadAd(context: Context) {
        if (_isAdLoaded.value || _isLoading.value) {
            Timber.d("Rewarded ad already loaded or currently loading.")
            return
        }

        _isLoading.value = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = adUnitIdProvider()

        Timber.d("Preloading rewarded ad with unit ID: $adUnitId")

        RewardedAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Timber.i("Rewarded ad loaded successfully.")
                    currentRewardedAd = ad
                    _isAdLoaded.value = true
                    _isLoading.value = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Timber.w("Rewarded ad failed to load: code=${loadAdError.code}, message=${loadAdError.message}")
                    currentRewardedAd = null
                    _isAdLoaded.value = false
                    _isLoading.value = false
                }
            }
        )
    }

    override fun showAd(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdFailed: (errorMessage: String) -> Unit
    ) {
        val ad = currentRewardedAd
        if (ad == null) {
            Timber.w("showAd called but no ad is loaded.")
            onAdFailed("Ad is loading or unavailable. Please check your internet connection and try again.")
            preloadAd(activity)
            return
        }

        var rewardGranted = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Timber.d("Rewarded ad showed full screen content.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Timber.e("Rewarded ad failed to show: ${adError.message}")
                currentRewardedAd = null
                _isAdLoaded.value = false
                preloadAd(activity)
                onAdFailed(adError.message)
            }

            override fun onAdDismissedFullScreenContent() {
                Timber.d("Rewarded ad dismissed. Reward earned: $rewardGranted")
                currentRewardedAd = null
                _isAdLoaded.value = false
                // Preload the next ad in the background
                preloadAd(activity)

                if (rewardGranted) {
                    onUserEarnedReward()
                } else {
                    onAdFailed("Ad was closed before completion. Reward was not granted.")
                }
            }
        }

        ad.show(activity) { rewardItem ->
            Timber.i("OnUserEarnedReward: type=${rewardItem.type}, amount=${rewardItem.amount}")
            rewardGranted = true
        }
    }
}
