package com.tailorbook

import com.tailorbook.feature.calendar.domain.model.CalendarDay
import com.tailorbook.feature.calendar.presentation.CalendarViewModel
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.domain.repository.CustomerRepository
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.domain.repository.OrderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for Delivery Calendar calculation and daily schedule (TASK-09-01).
 */
class CalendarViewModelTest {

    private lateinit var fakeOrderDao: FakeOrderDao
    private lateinit var orderRepository: OrderRepository
    private lateinit var fakeCustomerRepository: FakeCalendarCustomerRepository
    private lateinit var viewModel: CalendarViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        fakeOrderDao = FakeOrderDao()
        orderRepository = com.tailorbook.feature.orders.data.repository.OrderRepositoryImpl(fakeOrderDao)
        fakeCustomerRepository = FakeCalendarCustomerRepository()
        viewModel = CalendarViewModel(orderRepository, fakeCustomerRepository, "shop-1", testScope)
    }

    @Test
    fun getStartOfDay_normalizesToMidnight() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 17)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 30)
        }
        val normalized = CalendarViewModel.getStartOfDay(cal.timeInMillis)

        val checkCal = Calendar.getInstance().apply { timeInMillis = normalized }
        assertEquals(0, checkCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, checkCal.get(Calendar.MINUTE))
        assertEquals(0, checkCal.get(Calendar.SECOND))
        assertEquals(0, checkCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun computeMonthDays_generatesCompleteGrid() {
        val testCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        val days = CalendarViewModel.computeMonthDays(testCal, emptyList())

        // Grid must be 35 or 42 cells (multiple of 7)
        assertTrue(days.size == 35 || days.size == 42)
        assertEquals(0, days.size % 7)

        val novemberDays = days.filter { it.isCurrentMonth }
        assertEquals(30, novemberDays.size) // November has 30 days
    }

    @Test
    fun scheduledOrders_filteredBySelectedDate() = runBlocking {
        val todayStart = CalendarViewModel.getStartOfDay(System.currentTimeMillis())
        val tomorrowStart = todayStart + 86400000L

        val cust = CustomerEntity(id = "cust-1", shopId = "shop-1", name = "Priya Sharma", primaryPhone = "9876543210")
        fakeCustomerRepository.customers.add(cust)

        val order1 = OrderEntity(
            id = "ord-1",
            shopId = "shop-1",
            orderNumber = "TB-101",
            customerId = "cust-1",
            promisedDeliveryDate = todayStart + 3600000L * 14, // 2 PM today
            status = "READY",
            totalAmount = 1000.0,
            balanceAmount = 400.0
        )
        val order2 = OrderEntity(
            id = "ord-2",
            shopId = "shop-1",
            orderNumber = "TB-102",
            customerId = "cust-1",
            promisedDeliveryDate = tomorrowStart + 3600000L * 16, // 4 PM tomorrow
            status = "STITCHING",
            totalAmount = 1500.0,
            balanceAmount = 1500.0
        )

        fakeOrderDao.insertOrder(order1)
        fakeOrderDao.insertOrder(order2)

        val vm = CalendarViewModel(orderRepository, fakeCustomerRepository, "shop-1", testScope)
        vm.selectDate(todayStart)
        val scheduledToday = vm.scheduledOrdersForSelectedDate.first()

        assertEquals(1, scheduledToday.size)
        assertEquals("TB-101", scheduledToday[0].order.orderNumber)
        assertEquals("Priya Sharma", scheduledToday[0].customer?.name)

        // Select tomorrow
        vm.selectDate(tomorrowStart)
        val scheduledTomorrow = vm.scheduledOrdersForSelectedDate.first()

        assertEquals(1, scheduledTomorrow.size)
        assertEquals("TB-102", scheduledTomorrow[0].order.orderNumber)
    }

    @Test
    fun rescheduleOrder_updatesPromisedDateInCalendar() = runBlocking {
        val todayStart = CalendarViewModel.getStartOfDay(System.currentTimeMillis())
        val newDate = todayStart + 86400000L * 4

        val order = OrderEntity(
            id = "ord-resched",
            shopId = "shop-1",
            orderNumber = "TB-103",
            customerId = "cust-1",
            promisedDeliveryDate = todayStart,
            status = "NEW",
            totalAmount = 800.0,
            balanceAmount = 800.0
        )
        fakeOrderDao.insertOrder(order)

        viewModel.openRescheduleDialog(order)
        viewModel.setRescheduleDate(newDate)
        viewModel.setRescheduleReason("Cloth arrived late from vendor")
        viewModel.confirmReschedule()

        val updated = fakeOrderDao.getOrderById("ord-resched")
        assertEquals(newDate, updated?.promisedDeliveryDate)
    }
}

/**
 * Fake Customer repository for testing calendar.
 */
class FakeCalendarCustomerRepository : CustomerRepository {
    val customers = mutableListOf<CustomerEntity>()

    override fun getActiveCustomers(shopId: String): Flow<List<CustomerEntity>> = flowOf(customers)
    override fun searchCustomers(query: String, shopId: String): Flow<List<CustomerEntity>> = flowOf(customers)
    override suspend fun getCustomerById(customerId: String): CustomerEntity? = customers.find { it.id == customerId }
    override fun getCustomerWithFamilyMembers(customerId: String): Flow<com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers?> = flowOf(null)
    override suspend fun saveCustomer(customer: CustomerEntity): Long {
        customers.add(customer)
        return 1L
    }
    override suspend fun updateCustomer(customer: CustomerEntity) {
        val idx = customers.indexOfFirst { it.id == customer.id }
        if (idx != -1) customers[idx] = customer
    }
    override suspend fun archiveCustomer(customerId: String) { customers.removeIf { it.id == customerId } }
    override suspend fun addFamilyMember(member: com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity): Long = 1L
    override suspend fun updateFamilyMember(member: com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity) {}
    override suspend fun deleteFamilyMember(memberId: String) {}
    override fun getFamilyMembersForCustomer(customerId: String): Flow<List<com.tailorbook.feature.customers.data.local.entity.FamilyMemberEntity>> = flowOf(emptyList())
    override fun getAllCustomersWithFamilyMembers(shopId: String): Flow<List<com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers>> =
        flowOf(customers.filter { shopId.isBlank() || it.shopId == shopId }.map { com.tailorbook.feature.customers.data.local.entity.CustomerWithFamilyMembers(it, emptyList()) })
}
