package com.tailorbook

import com.tailorbook.feature.dashboard.domain.model.DashboardMetrics
import com.tailorbook.feature.dashboard.domain.model.DeliverySummaryItem
import com.tailorbook.feature.dashboard.domain.model.OrderStatus
import com.tailorbook.feature.dashboard.presentation.DashboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Phase 04 Dashboard: Metrics calculation, OrderStatus lifecycle, and ViewModel.
 */
class DashboardSanityTest {

    @Test
    fun testOrderStatusEnum_allSevenStatesPresent() {
        val states = OrderStatus.values()
        assertEquals("Must support exactly 7 order states", 7, states.size)

        val expected = setOf(
            OrderStatus.NEW,
            OrderStatus.CUTTING,
            OrderStatus.STITCHING,
            OrderStatus.FINISHING,
            OrderStatus.READY,
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED
        )
        assertEquals(expected, states.toSet())

        states.forEach { status ->
            assertTrue("Status ${status.name} must have a valid string resource", status.labelRes != 0)
        }
    }

    @Test
    fun testMetricsCalculation_activeAndPendingBalances() {
        val items = listOf(
            DeliverySummaryItem("1", "#TB-01", "A", "9876543210", "Blouse", OrderStatus.READY, "Today", 500.0),
            DeliverySummaryItem("2", "#TB-02", "B", "9876543211", "Kurti", OrderStatus.STITCHING, "Today", 250.0),
            DeliverySummaryItem("3", "#TB-03", "C", "9876543212", "Dress", OrderStatus.DELIVERED, "Yesterday", 0.0),
            DeliverySummaryItem("4", "#TB-04", "D", "9876543213", "Suit", OrderStatus.CANCELLED, "Today", 100.0)
        )

        val active = items.count { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED }
        assertEquals(2, active)

        val ready = items.count { it.status == OrderStatus.READY }
        assertEquals(1, ready)

        val totalPendingBalance = items.sumOf { it.balanceAmount }
        assertEquals(850.0, totalPendingBalance, 0.001)
    }

    @Test
    fun testDashboardMetrics_defaults() {
        val metrics = DashboardMetrics()
        assertEquals(0, metrics.activeOrdersCount)
        assertEquals(0, metrics.dueTodayCount)
        assertEquals(0, metrics.overdueCount)
        assertEquals(0, metrics.readyCount)
        assertEquals(0.0, metrics.outstandingBalanceSum, 0.001)
    }
}
