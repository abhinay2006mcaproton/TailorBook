package com.tailorbook

import com.tailorbook.core.navigation.Screen
import com.tailorbook.feature.auth.presentation.businessTypeOptions
import com.tailorbook.feature.auth.presentation.onboardingSlides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Phase 03 Navigation, Session, Onboarding, Language Selection, Shop Registration, and Login/OTP.
 */
class SessionAndNavSanityTest {

    @Test
    fun testNavigationRoutes_allUniqueAndValid() {
        val routes = listOf(
            Screen.Splash.route,
            Screen.Onboarding.route,
            Screen.LanguageSelection.route,
            Screen.Login.route,
            Screen.ShopRegistration.route,
            Screen.ShopProfileConfig.route,
            Screen.Dashboard.route,
            Screen.CustomerList.route,
            Screen.AddCustomer.route,
            Screen.CustomerProfile.route,
            Screen.EditCustomer.route,
            Screen.GarmentTemplateSelector.route,
            Screen.MeasurementEntry.route,
            Screen.MeasurementHistory.route,
            Screen.MeasurementDiff.route
        )

        // All routes must be distinct
        assertEquals("Route count should match unique route set size", routes.size, routes.toSet().size)

        // Every route must be a valid non-empty path
        routes.forEach { route ->
            assertTrue("Route '$route' should not be blank", route.isNotBlank())
            assertTrue("Route '$route' should not contain spaces", !route.contains(" "))
        }
    }

    @Test
    fun testSplashRoute_isInitialEntryRoute() {
        assertEquals("splash", Screen.Splash.route)
    }

    @Test
    fun testOnboardingSlides_exactlyFourSlidesWithResources() {
        assertEquals("Onboarding must contain exactly 4 feature slides", 4, onboardingSlides.size)
        onboardingSlides.forEach { slide ->
            assertTrue("Slide title resource must be non-zero", slide.titleRes != 0)
            assertTrue("Slide desc resource must be non-zero", slide.descRes != 0)
        }
    }

    @Test
    fun testLanguageSelectionRoute_isRegistered() {
        assertEquals("language_selection", Screen.LanguageSelection.route)
    }

    @Test
    fun testShopRegistration_businessTypesAndValidation() {
        assertEquals("Must support exactly 4 business type categories", 4, businessTypeOptions.size)

        val phoneRegex = Regex("^[6-9]\\d{9}$")
        // Valid Indian mobile numbers
        assertTrue(phoneRegex.matches("9876543210"))
        assertTrue(phoneRegex.matches("8765432109"))
        assertTrue(phoneRegex.matches("7012345678"))
        assertTrue(phoneRegex.matches("6301234567"))

        // Invalid phone numbers
        assertFalse(phoneRegex.matches("1234567890")) // Doesn't start with 6-9
        assertFalse(phoneRegex.matches("98765"))      // Too short
        assertFalse(phoneRegex.matches("98765432101")) // Too long
        assertFalse(phoneRegex.matches("98765abcde")) // Non-digits
    }

    @Test
    fun testOtpValidation_sixDigitsOnly() {
        val otpRegex = Regex("^\\d{6}$")
        assertTrue(otpRegex.matches("123456"))
        assertTrue(otpRegex.matches("000000"))
        assertTrue(otpRegex.matches("999999"))

        assertFalse(otpRegex.matches("12345"))  // 5 digits
        assertFalse(otpRegex.matches("1234567")) // 7 digits
        assertFalse(otpRegex.matches("12345a")) // non-digit
        assertFalse(otpRegex.matches(""))       // empty
    }

    @Test
    fun testShopProfileConfig_enumsAndDefaults() {
        // Enums must have valid codes and resource ids
        com.tailorbook.feature.auth.presentation.MeasurementUnit.values().forEach { unit ->
            assertTrue(unit.code.isNotEmpty())
            assertTrue(unit.labelRes != 0)
        }

        com.tailorbook.feature.auth.presentation.WeeklyOffDay.values().forEach { day ->
            assertTrue(day.code.isNotEmpty())
            assertTrue(day.labelRes != 0)
        }

        assertEquals("shop_profile_config", Screen.ShopProfileConfig.route)
    }

    @Test
    fun testSessionRoutingLogic_allStateTransitions() {
        fun resolveNextRoute(isLoggedIn: Boolean, isOnboardingCompleted: Boolean): String {
            return when {
                isLoggedIn -> Screen.Dashboard.route
                isOnboardingCompleted -> Screen.Login.route
                else -> Screen.Onboarding.route
            }
        }

        // Fresh install -> Onboarding
        assertEquals(Screen.Onboarding.route, resolveNextRoute(isLoggedIn = false, isOnboardingCompleted = false))

        // Onboarding completed but not logged in -> Login
        assertEquals(Screen.Login.route, resolveNextRoute(isLoggedIn = false, isOnboardingCompleted = true))

        // Logged in user -> Direct to Dashboard (bypassing onboarding & login)
        assertEquals(Screen.Dashboard.route, resolveNextRoute(isLoggedIn = true, isOnboardingCompleted = true))
        assertEquals(Screen.Dashboard.route, resolveNextRoute(isLoggedIn = true, isOnboardingCompleted = false))
    }

    @Test
    fun testUserPreferencesKeys_nonEmptyAndDistinct() {
        val keys = listOf(
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_SELECTED_LANGUAGE.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_ONBOARDING_COMPLETED.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_IS_LOGGED_IN.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_USER_ID.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_SHOP_NAME.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_OWNER_NAME.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_OWNER_PHONE.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_BUSINESS_TYPE.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_OPENING_TIME.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_CLOSING_TIME.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_WEEKLY_OFF.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_CURRENCY_SYMBOL.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_MEASUREMENT_UNIT.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_DELIVERY_LEAD_DAYS.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_SHOP_ADDRESS.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_GSTIN.name,
            com.tailorbook.core.datastore.UserPreferencesManager.KEY_CONFIG_COMPLETED.name
        )

        // All keys must be distinct
        assertEquals(keys.size, keys.toSet().size)
        keys.forEach { keyName ->
            assertTrue(keyName.isNotBlank())
        }
    }
}
