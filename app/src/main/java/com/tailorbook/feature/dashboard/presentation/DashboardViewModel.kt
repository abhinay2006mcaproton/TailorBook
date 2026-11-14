package com.tailorbook.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.customers.data.local.dao.CustomerDao
import com.tailorbook.feature.dashboard.domain.model.DashboardMetrics
import com.tailorbook.feature.dashboard.domain.model.DashboardUiState
import com.tailorbook.feature.dashboard.domain.model.DeliverySummaryItem
import com.tailorbook.feature.dashboard.domain.model.OrderStatus
import com.tailorbook.feature.orders.data.local.dao.OrderDao
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ViewModel for managing the TailorBook Dashboard state and metrics calculation.
 * Ensures metrics and delivery queues are reactively computed and updated in both DEBUG and RELEASE builds.
 */
class DashboardViewModel(
    private val orderDao: OrderDao? = null,
    private val customerDao: CustomerDao? = null,
    initialShopId: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _deliveries = MutableStateFlow<List<DeliverySummaryItem>>(emptyList())
    val deliveries: StateFlow<List<DeliverySummaryItem>> = _deliveries.asStateFlow()

    private val _activeShopId = MutableStateFlow(initialShopId)
    val activeShopId: StateFlow<String> = _activeShopId.asStateFlow()

    fun setActiveShopId(newShopId: String) {
        val trimmed = newShopId.trim()
        if (_activeShopId.value != trimmed) {
            _activeShopId.value = trimmed
        }
    }

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            if (orderDao != null) {
                _activeShopId.collectLatest { sId ->
                    orderDao.getAllOrdersWithDetails(sId).collectLatest { ordersWithDetails ->
                        if (ordersWithDetails.isNotEmpty()) {
                            val activeOrders = ordersWithDetails.filter {
                                it.order.status != "DELIVERED" && it.order.status != "CANCELLED"
                            }
                            val todayStart = getStartOfTodayMs()
                            val todayEnd = todayStart + 86400000L
                            val dueToday = activeOrders.count { it.order.promisedDeliveryDate in todayStart..todayEnd }
                            val overdue = activeOrders.count { it.order.promisedDeliveryDate > 0 && it.order.promisedDeliveryDate < todayStart }
                            val ready = activeOrders.count { it.order.status == "READY" }
                            val pendingBalance = activeOrders.sumOf { it.order.balanceAmount }

                            val metrics = DashboardMetrics(
                                activeOrdersCount = activeOrders.size,
                                dueTodayCount = dueToday,
                                overdueCount = overdue,
                                readyCount = ready,
                                outstandingBalanceSum = pendingBalance
                            )

                            // Sort active orders by delivery date ascending for today's deliveries section
                            val liveDeliveries = activeOrders
                                .sortedBy { it.order.promisedDeliveryDate }
                                .take(10)
                                .map { orderItem ->
                                    val cust = try {
                                        customerDao?.getCustomerById(orderItem.order.customerId)
                                    } catch (_: Exception) {
                                        null
                                    }
                                    val custName = cust?.name ?: "Customer"
                                    val custPhone = cust?.primaryPhone ?: ""
                                    val garmentType = orderItem.items.joinToString(", ") { "${it.quantity}x ${it.garmentName}" }
                                        .ifBlank { "Custom Order" }
                                    val status = try {
                                        OrderStatus.valueOf(orderItem.order.status)
                                    } catch (_: Exception) {
                                        OrderStatus.NEW
                                    }
                                    val deliveryFormatted = formatDeliveryDate(orderItem.order.promisedDeliveryDate, todayStart, todayEnd)

                                    DeliverySummaryItem(
                                        orderId = orderItem.order.id,
                                        orderNumber = if (orderItem.order.orderNumber.startsWith("#")) orderItem.order.orderNumber else "#${orderItem.order.orderNumber}",
                                        customerName = custName,
                                        customerPhone = custPhone,
                                        garmentType = garmentType,
                                        status = status,
                                        deliveryDate = deliveryFormatted,
                                        balanceAmount = orderItem.order.balanceAmount
                                    )
                                }

                            _deliveries.value = liveDeliveries
                            _uiState.value = DashboardUiState.Success(
                                metrics = metrics,
                                todayDeliveries = liveDeliveries
                            )
                        } else {
                            val emptyMetrics = DashboardMetrics(
                                activeOrdersCount = 0,
                                dueTodayCount = 0,
                                overdueCount = 0,
                                readyCount = 0,
                                outstandingBalanceSum = 0.0
                            )
                            _deliveries.value = emptyList()
                            _uiState.value = DashboardUiState.Success(
                                metrics = emptyMetrics,
                                todayDeliveries = emptyList()
                            )
                        }
                    }
                }
            } else {
                // In standalone preview / mock tests without DAO
                val sampleDeliveries = listOf(
                    DeliverySummaryItem(
                        orderId = "ord-001",
                        orderNumber = "#TB-101",
                        customerName = "Ananya Reddy",
                        customerPhone = "9876543210",
                        garmentType = "Silk Bridal Blouse",
                        status = OrderStatus.READY,
                        deliveryDate = "Today, 5:00 PM",
                        balanceAmount = 450.0
                    ),
                    DeliverySummaryItem(
                        orderId = "ord-002",
                        orderNumber = "#TB-102",
                        customerName = "Lakshmi Devi",
                        customerPhone = "9848012345",
                        garmentType = "Princess Cut Kurti (2 Pcs)",
                        status = OrderStatus.STITCHING,
                        deliveryDate = "Today, 7:00 PM",
                        balanceAmount = 300.0
                    )
                )
                _deliveries.value = sampleDeliveries
                recalculateMetrics(sampleDeliveries)
            }
        }
    }

    private fun formatDeliveryDate(timestamp: Long, todayStart: Long, todayEnd: Long): String {
        if (timestamp <= 0) return "Not Scheduled"
        return when {
            timestamp in todayStart..todayEnd -> "Today"
            timestamp < todayStart -> {
                val daysAgo = ((todayStart - timestamp) / 86400000L).coerceAtLeast(1)
                "Overdue ($daysAgo d)"
            }
            timestamp in (todayEnd)..(todayEnd + 86400000L) -> "Tomorrow"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun getStartOfTodayMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            orderDao?.updateOrderStatus(orderId, newStatus.name, System.currentTimeMillis())
            val updated = _deliveries.value.map { item ->
                if (item.orderId == orderId) item.copy(status = newStatus) else item
            }
            _deliveries.value = updated
            recalculateMetrics(updated)
        }
    }

    private fun recalculateMetrics(deliveries: List<DeliverySummaryItem>) {
        val active = deliveries.count { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED }
        val dueToday = deliveries.count { it.deliveryDate.contains("Today", ignoreCase = true) }
        val overdue = deliveries.count { it.deliveryDate.contains("Overdue", ignoreCase = true) }
        val ready = deliveries.count { it.status == OrderStatus.READY }
        val pendingBalance = deliveries.sumOf { it.balanceAmount }

        val metrics = DashboardMetrics(
            activeOrdersCount = active,
            dueTodayCount = dueToday,
            overdueCount = overdue,
            readyCount = ready,
            outstandingBalanceSum = pendingBalance
        )

        _uiState.value = DashboardUiState.Success(
            metrics = metrics,
            todayDeliveries = deliveries
        )
    }
}
