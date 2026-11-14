package com.tailorbook.ui.components

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.ump.UserMessagingPlatform
import com.tailorbook.core.ads.AdEligibilityProvider
import com.tailorbook.core.ads.AdMobConfig
import com.tailorbook.core.ads.AdPlacement
import com.tailorbook.core.ads.AdaptiveBannerCalculator
import com.tailorbook.core.ads.DefaultAdEligibilityProvider
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.MutedSage
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reusable Anchored Adaptive Banner Ad Composable.
 *
 * Implements:
 * - Gap 1: Official Google Mobile Ads anchored adaptive banner sizing based on Compose container width.
 * - Gap 2: Full lifecycle management (resume on ON_RESUME, pause on ON_PAUSE, single-call destroy on ON_DESTROY/dispose).
 * - Container collapse state machine: collapses completely on failure (zero dp footprint, no blank whitespace).
 * - Gap 3: Centralized placement configuration & eligibility checking.
 */
@Composable
fun TailorAdaptiveBannerAd(
    placement: AdPlacement,
    isPremium: Boolean,
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.getBannerAdUnitId(),
    adEligibilityProvider: AdEligibilityProvider = DefaultAdEligibilityProvider
) {
    val context = LocalContext.current

    // Query UMP consent safely
    val canRequestAds = remember(context) {
        if (com.tailorbook.BuildConfig.DEBUG) {
            true
        } else {
            try {
                UserMessagingPlatform.getConsentInformation(context).canRequestAds()
            } catch (_: Throwable) {
                true // Fallback in non-UMP environments
            }
        }
    }

    // Gate: Check master eligibility before allocating view hierarchy or requesting ads
    if (!adEligibilityProvider.isEligibleForAd(placement, isPremium, canRequestAds)) {
        return
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val configuration = LocalConfiguration.current
        val orientation = configuration.orientation

        // Compute available width in dp from Compose container constraints
        val containerWidthDp = maxWidth.value.toInt()
        if (!AdaptiveBannerCalculator.isValidWidth(containerWidthDp)) {
            return@BoxWithConstraints
        }

        // Calculate anchored adaptive banner ad size using official Google SDK API
        val adSize = remember(containerWidthDp, orientation) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, containerWidthDp)
        }

        var isAdFailed by remember(adSize, adUnitId) { mutableStateOf(false) }

        if (isAdFailed) {
            // Completely collapse container on failure: zero dp footprint
            return@BoxWithConstraints
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        var adViewRef by remember { mutableStateOf<AdView?>(null) }
        val isDestroyed = remember { AtomicBoolean(false) }

        // Manage Android lifecycle: Resume on ON_RESUME, pause on ON_PAUSE, destroy on ON_DESTROY/dispose exactly once
        DisposableEffect(lifecycleOwner, adViewRef) {
            val observer = LifecycleEventObserver { _, event ->
                val view = adViewRef ?: return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_RESUME -> view.resume()
                    Lifecycle.Event.ON_PAUSE -> view.pause()
                    Lifecycle.Event.ON_DESTROY -> {
                        if (!isDestroyed.getAndSet(true)) {
                            view.destroy()
                        }
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                adViewRef?.let { view ->
                    if (!isDestroyed.getAndSet(true)) {
                        view.destroy()
                    }
                }
            }
        }

        Surface(
            color = CardWhite,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .border(0.5.dp, MutedSage.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SPONSORED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = CharcoalSubtle.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(adSize)
                            this.adUnitId = adUnitId
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    isAdFailed = false
                                    Timber.d("Anchored adaptive banner loaded for placement $placement ($adSize)")
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    isAdFailed = true
                                    Timber.w("Banner ad failed for $placement. Code: ${error.code}, message: ${error.message}")
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                            adViewRef = this
                        }
                    }
                )
            }
        }
    }
}

/**
 * Backward-compatible container delegate for existing callers (e.g. ReportsScreen).
 */
@Composable
fun TailorAdBanner(
    isPremium: Boolean,
    onUpgradeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TailorAdaptiveBannerAd(
        placement = AdPlacement.REPORTS,
        isPremium = isPremium,
        modifier = modifier
    )
}
