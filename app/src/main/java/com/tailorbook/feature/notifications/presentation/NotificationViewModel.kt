package com.tailorbook.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.domain.repository.CustomerRepository
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class NotificationType {
    DUE_TODAY,
    DUE_TOMORROW,
    OVERDUE,
    PAYMENT_DUE
}

data class BusinessNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val orderId: String? = null,
    val customerId: String? = null,
    val customerPhone: String? = null
)

/**
 * ViewModel aggregating active operational alerts and client reminders (TASK-11-02 / REQ-NOTIF-01).
 */
class NotificationViewModel(
    val orderRepository: OrderRepository,
    val customerRepository: CustomerRepository,
    val shopId: String = "shop-001",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _readNotificationIds = MutableStateFlow<Set<String>>(emptySet())
    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()

    val allOrders: StateFlow<List<OrderEntity>> = orderRepository.getAllOrders(shopId)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val allCustomers: StateFlow<List<CustomerEntity>> = customerRepository.getActiveCustomers(shopId)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val activeNotifications: StateFlow<List<BusinessNotification>> = combine(
        allOrders,
        allCustomers,
        _readNotificationIds
    ) { orders, customers, readIds ->
        generateNotifications(orders, customers, readIds)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun markAsRead(id: String) {
        _readNotificationIds.value = _readNotificationIds.value + id
    }

    fun markAllAsRead() {
        val currentIds = activeNotifications.value.map { it.id }.toSet()
        _readNotificationIds.value = _readNotificationIds.value + currentIds
    }

    fun generateNotifications(
        orders: List<OrderEntity>,
        customers: List<CustomerEntity>,
        readIds: Set<String>
    ): List<BusinessNotification> {
        val customerMap = customers.associateBy { it.id }
        val now = System.currentTimeMillis()
        val startOfToday = getStartOfDay(now)
        val endOfToday = startOfToday + 86400000L
        val endOfTomorrow = endOfToday + 86400000L

        val list = mutableListOf<BusinessNotification>()

        orders.filter { it.status != "CANCELLED" && it.status != "DELIVERED" }.forEach { order ->
            val customer = customerMap[order.customerId]
            val clientName = customer?.name ?: "Client"
            val phone = customer?.primaryPhone

            when {
                // Overdue
                order.promisedDeliveryDate < startOfToday -> {
                    val notifId = "overdue-${order.id}"
                    list.add(
                        BusinessNotification(
                            id = notifId,
                            type = NotificationType.OVERDUE,
                            title = "Delivery Overdue: #${order.orderNumber}",
                            message = "$clientName's order promised on time is currently pending completion (${order.status}).",
                            timestamp = order.promisedDeliveryDate,
                            isRead = readIds.contains(notifId),
                            orderId = order.id,
                            customerId = order.customerId,
                            customerPhone = phone
                        )
                    )
                }
                // Due Today
                order.promisedDeliveryDate in startOfToday until endOfToday -> {
                    val notifId = "today-${order.id}"
                    list.add(
                        BusinessNotification(
                            id = notifId,
                            type = NotificationType.DUE_TODAY,
                            title = "Delivery Due Today: #${order.orderNumber}",
                            message = "$clientName's order is scheduled for delivery today at 5:00 PM.",
                            timestamp = order.promisedDeliveryDate,
                            isRead = readIds.contains(notifId),
                            orderId = order.id,
                            customerId = order.customerId,
                            customerPhone = phone
                        )
                    )
                }
                // Due Tomorrow
                order.promisedDeliveryDate in endOfToday until endOfTomorrow -> {
                    val notifId = "tomorrow-${order.id}"
                    list.add(
                        BusinessNotification(
                            id = notifId,
                            type = NotificationType.DUE_TOMORROW,
                            title = "Delivery Due Tomorrow: #${order.orderNumber}",
                            message = "$clientName's order is due tomorrow. Ensure stitching & finishing are ready.",
                            timestamp = order.promisedDeliveryDate,
                            isRead = readIds.contains(notifId),
                            orderId = order.id,
                            customerId = order.customerId,
                            customerPhone = phone
                        )
                    )
                }
            }

            // Payment Due Reminder
            if (order.balanceAmount > 0 && order.status in listOf("READY", "DELIVERED")) {
                val notifId = "bal-${order.id}"
                list.add(
                    BusinessNotification(
                        id = notifId,
                        type = NotificationType.PAYMENT_DUE,
                        title = "Payment Due: ₹${order.balanceAmount.toInt()} ($clientName)",
                        message = "Order #${order.orderNumber} is ${order.status.lowercase()} with pending balance of ₹${order.balanceAmount.toInt()}.",
                        timestamp = order.orderDate,
                        isRead = readIds.contains(notifId),
                        orderId = order.id,
                        customerId = order.customerId,
                        customerPhone = phone
                    )
                )
            }
        }

        return list.sortedWith(
            compareBy<BusinessNotification> { it.isRead }
                .thenBy {
                    when (it.type) {
                        NotificationType.OVERDUE -> 1
                        NotificationType.DUE_TODAY -> 2
                        NotificationType.DUE_TOMORROW -> 3
                        NotificationType.PAYMENT_DUE -> 4
                    }
                }
        )
    }

    private fun getStartOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
