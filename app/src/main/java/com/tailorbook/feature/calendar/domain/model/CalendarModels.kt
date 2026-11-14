package com.tailorbook.feature.calendar.domain.model

import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.orders.data.local.entity.OrderEntity

/**
 * Domain model representing a single calendar day in the monthly grid.
 * (TASK-09-01 / REQ-CAL-01 / REQ-CAL-02).
 */
data class CalendarDay(
    val timestamp: Long,            // Start of day in millis
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val orders: List<OrderEntity> = emptyList()
) {
    val activeOrders: List<OrderEntity>
        get() = orders.filter { it.status != "DELIVERED" && it.status != "CANCELLED" }

    val deliveredOrders: List<OrderEntity>
        get() = orders.filter { it.status == "DELIVERED" }

    val hasDeliveries: Boolean
        get() = orders.isNotEmpty()

    val totalCount: Int
        get() = orders.size

    val activeCount: Int
        get() = activeOrders.size

    val deliveredCount: Int
        get() = deliveredOrders.size

    fun hasOverdue(startOfTodayMillis: Long): Boolean {
        return timestamp < startOfTodayMillis && activeOrders.isNotEmpty()
    }
}

/**
 * Scheduled order representation combining Order and Customer information
 * for instant dialer/WhatsApp actions in the daily schedule.
 */
data class ScheduledOrder(
    val order: OrderEntity,
    val customer: CustomerEntity?
)
