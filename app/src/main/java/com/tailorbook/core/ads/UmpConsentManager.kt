package com.tailorbook.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import timber.log.Timber

/**
 * Interface contract for managing GDPR / CCPA privacy consent via Google UMP SDK.
 */
interface ConsentManager {
    /** True if Google allows ads to be requested based on user consent status */
    val canRequestAds: Boolean

    /** True if user is in a regulated region (e.g. EEA) and should have access to privacy options form */
    val isPrivacyOptionsRequired: Boolean

    /**
     * Gathers user consent on app startup if required by privacy regulations.
     */
    fun gatherConsent(
        activity: Activity,
        isDebugEea: Boolean = false,
        testDeviceHashedId: String? = null,
        onConsentGathered: (canRequestAds: Boolean) -> Unit
    )

    /**
     * Displays privacy and consent options form (e.g. from app settings or drawer).
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onDismissed: () -> Unit = {}
    )
}

/**
 * Production implementation of [ConsentManager] using official Google User Messaging Platform SDK.
 */
class GoogleUmpConsentManager(
    private val context: Context
) : ConsentManager {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    override val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    override val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override fun gatherConsent(
        activity: Activity,
        isDebugEea: Boolean,
        testDeviceHashedId: String?,
        onConsentGathered: (canRequestAds: Boolean) -> Unit
    ) {
        val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
        if (isDebugEea) {
            debugSettingsBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        }
        if (!testDeviceHashedId.isNullOrBlank()) {
            debugSettingsBuilder.addTestDeviceHashedId(testDeviceHashedId)
        }

        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettingsBuilder.build())
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Timber.w("Consent form presentation error: code=${formError.errorCode}, msg=${formError.message}")
                    }

                    val canRequest = consentInformation.canRequestAds()
                    Timber.i("Consent gathered successfully. canRequestAds=$canRequest")

                    // Automatically initialize AdMob if consent is granted or not required
                    if (canRequest) {
                        AdMobInitializer.initialize(activity)
                    }

                    onConsentGathered(canRequest)
                }
            },
            { requestConsentError ->
                Timber.e("Failed to update consent information: code=${requestConsentError.errorCode}, msg=${requestConsentError.message}")
                onConsentGathered(consentInformation.canRequestAds())
            }
        )
    }

    override fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Timber.w("Error showing privacy options form: ${formError.message}")
            }
            onDismissed()
        }
    }
}
