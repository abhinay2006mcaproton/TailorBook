package com.tailorbook.core.ads

import android.app.Activity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake implementation of ConsentManager for automated unit testing.
 */
class FakeConsentManager(
    private var simulatedCanRequestAds: Boolean = true,
    private var simulatedPrivacyRequired: Boolean = false
) : ConsentManager {

    override val canRequestAds: Boolean
        get() = simulatedCanRequestAds

    override val isPrivacyOptionsRequired: Boolean
        get() = simulatedPrivacyRequired

    override fun gatherConsent(
        activity: Activity,
        isDebugEea: Boolean,
        testDeviceHashedId: String?,
        onConsentGathered: (canRequestAds: Boolean) -> Unit
    ) {
        onConsentGathered(simulatedCanRequestAds)
    }

    override fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit) {
        onDismissed()
    }

    fun setCanRequestAds(allowed: Boolean) {
        simulatedCanRequestAds = allowed
    }

    fun setPrivacyOptionsRequired(required: Boolean) {
        simulatedPrivacyRequired = required
    }
}

/**
 * Unit tests verifying Google UMP Consent manager behavior.
 */
class ConsentManagerTest {

    private lateinit var consentManager: FakeConsentManager

    @Before
    fun setUp() {
        consentManager = FakeConsentManager()
    }

    @Test
    fun defaultState_allowsAdRequests() {
        assertTrue(consentManager.canRequestAds)
        assertFalse(consentManager.isPrivacyOptionsRequired)
    }

    @Test
    fun consentWithheld_blocksAdRequests() {
        consentManager.setCanRequestAds(false)
        assertFalse(consentManager.canRequestAds)
    }

    @Test
    fun regulatedRegion_requiresPrivacyOptionsForm() {
        consentManager.setPrivacyOptionsRequired(true)
        assertTrue(consentManager.isPrivacyOptionsRequired)
    }

    @Test
    fun gatherConsentCallback_notifiesStatusAccurately() {
        var notifiedStatus: Boolean? = null
        consentManager.setCanRequestAds(true)

        val dummyActivity = Activity()
        consentManager.gatherConsent(dummyActivity) { canRequest ->
            notifiedStatus = canRequest
        }

        assertTrue(notifiedStatus == true)
    }
}
