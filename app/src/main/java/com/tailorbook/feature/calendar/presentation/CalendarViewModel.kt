package com.tailorbook.feature.calendar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.calendar.domain.model.CalendarDay
import com.tailorbook.feature.calendar.domain.model.ScheduledOrder
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.domain.repository.CustomerRepository
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for the Delivery Calendar feature (TASK-09-01 / REQ-CAL-01).
 * Calculates monthly calendar grid, daily delivery queues, and handles rescheduling.
 */
class CalendarViewModel(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
    private val shopId: String = "shop-001",
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope: kotlinx.coroutines.CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _displayedCalendar = MutableStateFlow(Calendar.getInstance())
    val displayedCalendar: StateFlow<Calendar> = _displayedCalendar.asStateFlow()

    private val _selectedDateMillis = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL") // ALL, DUE, OVERDUE, DELIVERED
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    // Reschedule dialog state
    private val _orderToReschedule = MutableStateFlow<OrderEntity?>(null)
    val orderToReschedule: StateFlow<OrderEntity?> = _orderToReschedule.asStateFlow()

    private val _rescheduleNewDate = MutableStateFlow<Long>(System.currentTimeMillis() + 86400000L)
    val rescheduleNewDate: StateFlow<Long> = _rescheduleNewDate.asStateFlow()

    private val _rescheduleReason = MutableStateFlow("")
    val rescheduleReason: StateFlow<String> = _rescheduleReason.asStateFlow()

    val allOrders: StateFlow<List<OrderEntity>> = orderRepository.getAllOrders(shopId)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val allCustomers: StateFlow<List<CustomerEntity>> = customerRepository.getActiveCustomers(shopId)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Grid of 35 or 42 calendar day cells for the currently displayed month.
     */
    val monthDays: StateFlow<List<CalendarDay>> = combine(
        _displayedCalendar,
        allOrders
    ) { cal, orders ->
        computeMonthDays(cal, orders)
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Orders scheduled on the selected date, mapped with customer records and filtered by status.
     */
    val scheduledOrdersForSelectedDate: StateFlow<List<ScheduledOrder>> = combine(
        _selectedDateMillis,
        allOrders,
        allCustomers,
        _statusFilter
    ) { selectedDate, orders, customers, filter ->
        val customerMap = customers.associateBy { it.id }
        val startOfToday = getStartOfDay(System.currentTimeMillis())

        val dayOrders = orders.filter { order ->
            getStartOfDay(order.promisedDeliveryDate) == selectedDate
        }

        val filteredOrders = when (filter) {
            "DUE" -> dayOrders.filter { it.status != "DELIVERED" && it.status != "CANCELLED" }
            "OVERDUE" -> dayOrders.filter {
                it.status != "DELIVERED" && it.status != "CANCELLED" && selectedDate < startOfToday
            }
            "DELIVERED" -> dayOrders.filter { it.status == "DELIVERED" }
            else -> dayOrders.filter { it.status != "CANCELLED" }
        }

        filteredOrders.map { order ->
            ScheduledOrder(
                order = order,
                customer = customerMap[order.customerId]
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun previousMonth() {
        val cal = (_displayedCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        _displayedCalendar.value = cal
    }

    fun nextMonth() {
        val cal = (_displayedCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        _displayedCalendar.value = cal
    }

    fun goToToday() {
        val today = Calendar.getInstance()
        _displayedCalendar.value = today
        _selectedDateMillis.value = getStartOfDay(today.timeInMillis)
    }

    fun selectDate(timestampMillis: Long) {
        _selectedDateMillis.value = getStartOfDay(timestampMillis)
    }

    fun setFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun openRescheduleDialog(order: OrderEntity) {
        _orderToReschedule.value = order
        _rescheduleNewDate.value = order.promisedDeliveryDate + 86400000L * 2
        _rescheduleReason.value = ""
    }

    fun closeRescheduleDialog() {
        _orderToReschedule.value = null
    }

    fun setRescheduleDate(dateMillis: Long) {
        _rescheduleNewDate.value = dateMillis
    }

    fun addRescheduleDays(days: Int) {
        val baseDate = _orderToReschedule.value?.promisedDeliveryDate ?: System.currentTimeMillis()
        _rescheduleNewDate.value = baseDate + 86400000L * days
    }

    fun setRescheduleReason(reason: String) {
        _rescheduleReason.value = reason
    }

    fun confirmReschedule(onSuccess: (() -> Unit)? = null) {
        val order = _orderToReschedule.value ?: return
        val newDate = _rescheduleNewDate.value
        val reason = _rescheduleReason.value.trim().ifBlank { null }

        scope.launch {
            orderRepository.rescheduleOrderDelivery(order.id, newDate, reason)
            closeRescheduleDialog()
            onSuccess?.invoke()
        }
    }

    fun advanceOrderStatus(orderId: String, currentStatus: String, nextStatus: String) {
        scope.launch {
            orderRepository.updateOrderStatus(orderId, currentStatus, nextStatus, "Status updated from Delivery Calendar")
        }
    }

    companion object {
        fun getStartOfDay(timestamp: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun computeMonthDays(cal: Calendar, orders: List<OrderEntity>): List<CalendarDay> {
            val ordersByDay = orders.groupBy { getStartOfDay(it.promisedDeliveryDate) }
            val todayStart = getStartOfDay(System.currentTimeMillis())

            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            // Month start
            val monthCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday...
            val leadingDays = firstDayOfWeek - 1

            val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Start cursor from previous month leading days
            val cursor = (monthCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -leadingDays)
            }

            val totalCells = if (leadingDays + daysInMonth > 35) 42 else 35
            val daysList = mutableListOf<CalendarDay>()

            for (i in 0 until totalCells) {
                val dayMillis = cursor.timeInMillis
                val isCurMonth = cursor.get(Calendar.MONTH) == currentMonth
                val dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH)
                val isToday = dayMillis == todayStart
                val dayOrders = ordersByDay[dayMillis] ?: emptyList()

                daysList.add(
                    CalendarDay(
                        timestamp = dayMillis,
                        dayOfMonth = dayOfMonth,
                        isCurrentMonth = isCurMonth,
                        isToday = isToday,
                        orders = dayOrders
                    )
                )
                cursor.add(Calendar.DAY_OF_MONTH, 1)
            }

            return daysList
        }
    }
}
