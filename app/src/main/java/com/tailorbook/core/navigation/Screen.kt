package com.tailorbook.core.navigation

/**
 * Screen navigation routes for TailorBook.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object LanguageSelection : Screen("language_selection")
    object Login : Screen("login")
    object ShopRegistration : Screen("shop_registration")
    object ShopProfileConfig : Screen("shop_profile_config")
    object Dashboard : Screen("dashboard")
    object CustomerList : Screen("customer_list")
    object AddCustomer : Screen("add_customer")
    object CustomerProfile : Screen("customer_profile/{customerId}") {
        fun createRoute(customerId: String) = "customer_profile/$customerId"
    }
    object EditCustomer : Screen("edit_customer/{customerId}") {
        fun createRoute(customerId: String) = "edit_customer/$customerId"
    }

    object GarmentTemplateSelector : Screen("measurement_templates?customerId={customerId}&familyMemberId={familyMemberId}") {
        fun createRoute(customerId: String? = null, familyMemberId: String? = null): String {
            val cid = customerId?.ifBlank { null }
            val fmid = familyMemberId?.ifBlank { null }
            return when {
                cid != null && fmid != null -> "measurement_templates?customerId=$cid&familyMemberId=$fmid"
                cid != null -> "measurement_templates?customerId=$cid"
                else -> "measurement_templates"
            }
        }
    }

    object MeasurementEntry : Screen("measurement_entry/{customerId}/{templateId}?familyMemberId={familyMemberId}") {
        fun createRoute(customerId: String, templateId: String, familyMemberId: String? = null) =
            if (familyMemberId != null) "measurement_entry/$customerId/$templateId?familyMemberId=$familyMemberId"
            else "measurement_entry/$customerId/$templateId"
    }

    object MeasurementHistory : Screen("measurement_history/{customerId}/{templateId}?familyMemberId={familyMemberId}") {
        fun createRoute(customerId: String, templateId: String, familyMemberId: String? = null) =
            if (familyMemberId != null) "measurement_history/$customerId/$templateId?familyMemberId=$familyMemberId"
            else "measurement_history/$customerId/$templateId"
    }

    object MeasurementDiff : Screen("measurement_diff/{customerId}/{templateId}/{v1}/{v2}?familyMemberId={familyMemberId}") {
        fun createRoute(customerId: String, templateId: String, v1: Int, v2: Int, familyMemberId: String? = null) =
            if (familyMemberId != null) "measurement_diff/$customerId/$templateId/$v1/$v2?familyMemberId=$familyMemberId"
            else "measurement_diff/$customerId/$templateId/$v1/$v2"
    }

    // Phase 07: Design Gallery & Camera Module
    object DesignGallery : Screen("design_gallery")
    object PhotoDetails : Screen("photo_details/{photoId}") {
        fun createRoute(photoId: String) = "photo_details/$photoId"
    }

    object SelectCustomerForMeasurement : Screen("select_customer_for_measurement")

    // Phase 08: Order Management
    object OrderList : Screen("order_list")
    object OrderCreation : Screen("order_create?customerId={customerId}&templateId={templateId}") {
        fun createRoute(customerId: String? = null, templateId: String? = null): String {
            return when {
                customerId != null && templateId != null -> "order_create?customerId=$customerId&templateId=$templateId"
                customerId != null -> "order_create?customerId=$customerId"
                else -> "order_create"
            }
        }
    }
    object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: String) = "order_details/$orderId"
    }

    // Phase 09: Delivery Calendar
    object DeliveryCalendar : Screen("delivery_calendar")

    // Phase 10: Payments & Balances
    object PaymentList : Screen("payment_list")
    object RecordPayment : Screen("payment_record?orderId={orderId}&customerId={customerId}") {
        fun createRoute(orderId: String? = null, customerId: String? = null): String {
            return when {
                orderId != null && customerId != null -> "payment_record?orderId=$orderId&customerId=$customerId"
                orderId != null -> "payment_record?orderId=$orderId"
                customerId != null -> "payment_record?customerId=$customerId"
                else -> "payment_record"
            }
        }
    }

    // Phase 11: Reports & Notifications
    object Reports : Screen("reports")
    object Notifications : Screen("notifications")

    // Phase 12: Staff Management
    object StaffList : Screen("staff_list")

    // Phase 13: Subscription & Monetization
    object Subscription : Screen("subscription")
}
