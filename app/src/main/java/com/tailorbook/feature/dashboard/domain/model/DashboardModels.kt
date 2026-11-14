package com.tailorbook.feature.dashboard.domain.model

import com.tailorbook.R

/**
 * Standard 7-state lifecycle for tailoring orders.
 */
enum class OrderStatus(val labelRes: Int) {
    NEW(R.string.order_status_new),
    CUTTING(R.string.order_status_cutting),
    STITCHING(R.string.order_status_stitching),
    FINISHING(R.string.order_status_finishing),
    READY(R.string.order_status_ready),
    DELIVERED(R.string.order_status_delivered),
    CANCELLED(R.string.order_status_cancelled)
}

/**
 * Lightweight delivery summary item for the Dashboard carousel/list.
 */
data class DeliverySummaryItem(
    val orderId: String,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val garmentType: String,
    val status: OrderStatus,
    val deliveryDate: String,
    val balanceAmount: Double
)

/**
 * Core business metric aggregations for the tailor's shop.
 */
data class DashboardMetrics(
    val activeOrdersCount: Int = 0,
    val dueTodayCount: Int = 0,
    val overdueCount: Int = 0,
    val readyCount: Int = 0,
    val outstandingBalanceSum: Double = 0.0
)

/**
 * UI State for the Dashboard screen.
 */
sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val metrics: DashboardMetrics,
        val todayDeliveries: List<DeliverySummaryItem>,
        val isOffline: Boolean = false,
        val isSyncing: Boolean = false
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
