package com.tailorbook.feature.orders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.data.local.entity.OrderItemEntity
import com.tailorbook.feature.orders.data.local.entity.OrderMeasurementSnapshotEntity
import com.tailorbook.feature.orders.data.local.entity.OrderStatusHistoryEntity
import com.tailorbook.feature.orders.data.local.entity.OrderWithDetails
import com.tailorbook.feature.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing Orders, Status transitions, and Order Creation (TASK-08-01 / 03 / 04).
 */
class OrderViewModel(
    val repository: OrderRepository? = null,
    val initialShopId: String = "shop-001"
) : ViewModel() {

    private val _activeShopId = MutableStateFlow(initialShopId)
    val activeShopId: StateFlow<String> = _activeShopId

    val shopId: String get() = _activeShopId.value

    fun setActiveShopId(newShopId: String) {
        val trimmed = newShopId.trim()
        if (trimmed.isNotBlank() && _activeShopId.value != trimmed) {
            _activeShopId.value = trimmed
        }
    }

    private val _inMemoryOrders = MutableStateFlow<List<OrderWithDetails>>(emptyList())
    val inMemoryOrders: StateFlow<List<OrderWithDetails>> = _inMemoryOrders

    val activeOrderCount: StateFlow<Int> = _inMemoryOrders.map { orders ->
        orders.count { it.order.status != "DELIVERED" && it.order.status != "CANCELLED" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedStatusTab = MutableStateFlow("ALL")
    val selectedStatusTab: StateFlow<String> = _selectedStatusTab

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        if (repository != null) {
            viewModelScope.launch {
                _activeShopId.collectLatest { sId ->
                    repository.getAllOrdersWithDetails(sId).collect { roomOrders ->
                        _inMemoryOrders.value = roomOrders
                    }
                }
            }
        } else if (com.tailorbook.BuildConfig.DEBUG) {
            seedSampleOrdersIfEmpty()
        }
    }

    private fun seedSampleOrdersIfEmpty() {
        if (_inMemoryOrders.value.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            val order1 = OrderWithDetails(
                order = OrderEntity(
                    id = "ord-101",
                    shopId = shopId,
                    orderNumber = "TB-101",
                    customerId = "cust-101", // Ananya Reddy
                    orderDate = now - dayMs * 4,
                    promisedDeliveryDate = now + 3600000L * 2, // Today 5:00 PM
                    status = "READY",
                    totalAmount = 1450.0,
                    advanceAmount = 1000.0,
                    paidAmount = 1000.0,
                    balanceAmount = 450.0
                ),
                items = listOf(
                    OrderItemEntity(
                        id = "item-101",
                        orderId = "ord-101",
                        garmentTemplateId = "BLOUSE",
                        garmentName = "Silk Bridal Blouse",
                        quantity = 1,
                        unitPrice = 1450.0,
                        stitchingInstructions = "Deep pot neck with gold zari piping and tassels. Keep 1.5 inch margin inside.",
                        fabricDetails = "Pure Kanchipuram Raw Silk"
                    )
                ),
                measurementSnapshot = OrderMeasurementSnapshotEntity(
                    id = "snap-101",
                    orderId = "ord-101",
                    measurementVersionId = "v2",
                    frozenMeasurementsJson = "{\"length\":14.5,\"chest\":36.5,\"waist\":30.5,\"shoulder\":14.0,\"front_neck\":6.5,\"back_neck\":8.5,\"armhole\":16.5,\"cross_front\":13.0,\"cross_back\":14.0,\"apex_width\":7.0}",
                    unit = "INCH"
                ),
                statusHistory = listOf(
                    OrderStatusHistoryEntity(orderId = "ord-101", fromStatus = null, toStatus = "NEW", notes = "Order placed", timestamp = now - dayMs * 4),
                    OrderStatusHistoryEntity(orderId = "ord-101", fromStatus = "NEW", toStatus = "CUTTING", notes = "Pattern cut", timestamp = now - dayMs * 3),
                    OrderStatusHistoryEntity(orderId = "ord-101", fromStatus = "CUTTING", toStatus = "STITCHING", notes = "Stitching underway", timestamp = now - dayMs * 2),
                    OrderStatusHistoryEntity(orderId = "ord-101", fromStatus = "STITCHING", toStatus = "READY", notes = "Ironing & finishing completed", timestamp = now - 3600000L * 4)
                )
            )

            val order2 = OrderWithDetails(
                order = OrderEntity(
                    id = "ord-102",
                    shopId = shopId,
                    orderNumber = "TB-102",
                    customerId = "cust-102", // Lakshmi Devi
                    orderDate = now - dayMs * 2,
                    promisedDeliveryDate = now + 3600000L * 5, // Today 7:00 PM
                    status = "STITCHING",
                    totalAmount = 1100.0,
                    advanceAmount = 800.0,
                    paidAmount = 800.0,
                    balanceAmount = 300.0
                ),
                items = listOf(
                    OrderItemEntity(
                        id = "item-102",
                        orderId = "ord-102",
                        garmentTemplateId = "KURTI",
                        garmentName = "Princess Cut Kurti (2 Pcs)",
                        quantity = 2,
                        unitPrice = 550.0,
                        stitchingInstructions = "Boat neck with side slits and 3/4th sleeves.",
                        fabricDetails = "Cotton Chanderi with Lining"
                    )
                ),
                measurementSnapshot = OrderMeasurementSnapshotEntity(
                    id = "snap-102",
                    orderId = "ord-102",
                    measurementVersionId = "v1",
                    frozenMeasurementsJson = "{\"length\":42.0,\"bust\":36.0,\"waist\":32.0,\"hip\":38.0,\"shoulder\":14.5,\"sleeve_length\":16.0}",
                    unit = "INCH"
                ),
                statusHistory = listOf(
                    OrderStatusHistoryEntity(orderId = "ord-102", fromStatus = null, toStatus = "NEW", timestamp = now - dayMs * 2),
                    OrderStatusHistoryEntity(orderId = "ord-102", fromStatus = "NEW", toStatus = "CUTTING", timestamp = now - dayMs * 1),
                    OrderStatusHistoryEntity(orderId = "ord-102", fromStatus = "CUTTING", toStatus = "STITCHING", timestamp = now - 3600000L * 6)
                )
            )

            val order3 = OrderWithDetails(
                order = OrderEntity(
                    id = "ord-103",
                    shopId = shopId,
                    orderNumber = "TB-103",
                    customerId = "cust-103", // Sunitha Rao
                    orderDate = now - dayMs * 1,
                    promisedDeliveryDate = now + dayMs * 1, // Tomorrow 11:00 AM
                    status = "FINISHING",
                    totalAmount = 3200.0,
                    advanceAmount = 2000.0,
                    paidAmount = 2000.0,
                    balanceAmount = 1200.0
                ),
                items = listOf(
                    OrderItemEntity(
                        id = "item-103",
                        orderId = "ord-103",
                        garmentTemplateId = "DRESS",
                        garmentName = "Embroidered Lehenga & Choli",
                        quantity = 1,
                        unitPrice = 3200.0,
                        stitchingInstructions = "Scalloped waistband with heavy latkan bells on dori.",
                        fabricDetails = "Georgette with Heavy Embroidery"
                    )
                ),
                measurementSnapshot = OrderMeasurementSnapshotEntity(
                    id = "snap-103",
                    orderId = "ord-103",
                    measurementVersionId = "v1",
                    frozenMeasurementsJson = "{\"waist\":28.0,\"length\":40.0,\"flare\":120.0}",
                    unit = "INCH"
                ),
                statusHistory = listOf(
                    OrderStatusHistoryEntity(orderId = "ord-103", fromStatus = null, toStatus = "NEW", timestamp = now - dayMs * 1),
                    OrderStatusHistoryEntity(orderId = "ord-103", fromStatus = "NEW", toStatus = "FINISHING", timestamp = now - 3600000L * 3)
                )
            )

            val order4 = OrderWithDetails(
                order = OrderEntity(
                    id = "ord-104",
                    shopId = shopId,
                    orderNumber = "TB-100",
                    customerId = "cust-104", // Kavitha S.
                    orderDate = now - dayMs * 7,
                    promisedDeliveryDate = now - dayMs * 1,
                    actualDeliveryDate = now - dayMs * 1,
                    status = "DELIVERED",
                    totalAmount = 950.0,
                    advanceAmount = 950.0,
                    paidAmount = 950.0,
                    balanceAmount = 0.0
                ),
                items = listOf(
                    OrderItemEntity(
                        id = "item-104",
                        orderId = "ord-104",
                        garmentTemplateId = "BLOUSE",
                        garmentName = "Simple Cotton Blouse",
                        quantity = 1,
                        unitPrice = 950.0
                    )
                ),
                measurementSnapshot = OrderMeasurementSnapshotEntity(
                    id = "snap-104",
                    orderId = "ord-104",
                    measurementVersionId = "v1",
                    frozenMeasurementsJson = "{\"length\":14.0,\"chest\":34.0,\"waist\":28.0}",
                    unit = "INCH"
                ),
                statusHistory = listOf(
                    OrderStatusHistoryEntity(orderId = "ord-104", fromStatus = null, toStatus = "NEW", timestamp = now - dayMs * 7),
                    OrderStatusHistoryEntity(orderId = "ord-104", fromStatus = "READY", toStatus = "DELIVERED", timestamp = now - dayMs * 1)
                )
            )

            _inMemoryOrders.value = listOf(order1, order2, order3, order4)
        }
    }

    val filteredOrders: StateFlow<List<OrderWithDetails>> = combine(
        _inMemoryOrders,
        _selectedStatusTab,
        _searchQuery
    ) { orders, tab, query ->
        orders.filter { item ->
            val matchesTab = when (tab) {
                "ALL" -> true
                "NEW" -> item.order.status == "NEW"
                "IN_PROGRESS" -> item.order.status in listOf("CUTTING", "STITCHING", "FINISHING")
                "READY" -> item.order.status == "READY"
                "DELIVERED" -> item.order.status == "DELIVERED"
                else -> true
            }
            val matchesQuery = if (query.isBlank()) true else {
                item.order.orderNumber.contains(query, ignoreCase = true) ||
                        item.items.any { it.garmentName.contains(query, ignoreCase = true) }
            }
            matchesTab && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectStatusTab(tab: String) {
        _selectedStatusTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun getOrderDetails(orderId: String): OrderWithDetails? {
        return _inMemoryOrders.value.find { it.order.id == orderId }
    }

    fun getNextOrderNumber(): String {
        val count = _inMemoryOrders.value.size
        return "TB-%03d".format(count + 101)
    }

    data class OrderItemDraft(
        val id: String = UUID.randomUUID().toString(),
        val garmentTemplateId: String,
        val garmentName: String,
        val quantity: Int = 1,
        val unitPrice: Double,
        val stitchingInstructions: String? = null,
        val fabricDetails: String? = null,
        val frozenMeasurements: Map<String, Double> = emptyMap(),
        val unit: String = "INCH"
    )

    fun createMultiItemOrder(
        customerId: String,
        familyMemberId: String? = null,
        items: List<OrderItemDraft>,
        deliveryDate: Long,
        totalAmount: Double,
        advanceAmount: Double,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val orderId = UUID.randomUUID().toString()
            val orderNum = getNextOrderNumber()
            val balance = (totalAmount - advanceAmount).coerceAtLeast(0.0)

            val orderEntity = OrderEntity(
                id = orderId,
                shopId = shopId,
                orderNumber = orderNum,
                customerId = customerId,
                familyMemberId = familyMemberId,
                orderDate = System.currentTimeMillis(),
                promisedDeliveryDate = deliveryDate,
                status = "NEW",
                totalAmount = totalAmount,
                advanceAmount = advanceAmount,
                paidAmount = advanceAmount,
                balanceAmount = balance
            )

            val itemEntities = items.map { item ->
                val itemJsonBuilder = StringBuilder("{")
                item.frozenMeasurements.entries.forEachIndexed { i, entry ->
                    itemJsonBuilder.append("\"${entry.key}\":${entry.value}")
                    if (i < item.frozenMeasurements.size - 1) itemJsonBuilder.append(",")
                }
                itemJsonBuilder.append("}")

                OrderItemEntity(
                    id = item.id,
                    orderId = orderId,
                    garmentTemplateId = item.garmentTemplateId,
                    garmentName = item.garmentName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    stitchingInstructions = item.stitchingInstructions,
                    fabricDetails = item.fabricDetails,
                    measurementsJson = itemJsonBuilder.toString(),
                    unit = item.unit
                )
            }

            val primaryItem = items.firstOrNull()
            val jsonBuilder = StringBuilder("{")
            primaryItem?.frozenMeasurements?.entries?.forEachIndexed { i, entry ->
                jsonBuilder.append("\"${entry.key}\":${entry.value}")
                if (i < primaryItem.frozenMeasurements.size - 1) jsonBuilder.append(",")
            }
            jsonBuilder.append("}")

            val snapshotEntity = OrderMeasurementSnapshotEntity(
                id = UUID.randomUUID().toString(),
                orderId = orderId,
                measurementVersionId = "snap",
                frozenMeasurementsJson = jsonBuilder.toString(),
                unit = primaryItem?.unit ?: "INCH"
            )

            val summaryList = items.joinToString(", ") { "${it.quantity}x ${it.garmentName}" }
            val history = OrderStatusHistoryEntity(
                orderId = orderId,
                fromStatus = null,
                toStatus = "NEW",
                notes = if (summaryList.isNotBlank()) "Order booked: $summaryList" else "Order booked"
            )

            val newOrderWithDetails = OrderWithDetails(
                order = orderEntity,
                items = itemEntities,
                measurementSnapshot = snapshotEntity,
                statusHistory = listOf(history)
            )

            _inMemoryOrders.value = listOf(newOrderWithDetails) + _inMemoryOrders.value
            repository?.createOrderWithSnapshot(orderEntity, itemEntities, snapshotEntity)
            onSuccess(orderId)
        }
    }

    fun createOrder(
        customerId: String,
        familyMemberId: String? = null,
        garmentTemplateId: String,
        garmentName: String,
        instructions: String,
        fabricDetails: String,
        deliveryDate: Long,
        totalAmount: Double,
        advanceAmount: Double,
        frozenMeasurements: Map<String, Double>,
        unit: String,
        onSuccess: (String) -> Unit
    ) {
        val singleItem = OrderItemDraft(
            garmentTemplateId = garmentTemplateId,
            garmentName = garmentName,
            quantity = 1,
            unitPrice = totalAmount,
            stitchingInstructions = instructions,
            fabricDetails = fabricDetails,
            frozenMeasurements = frozenMeasurements,
            unit = unit
        )
        createMultiItemOrder(
            customerId = customerId,
            familyMemberId = familyMemberId,
            items = listOf(singleItem),
            deliveryDate = deliveryDate,
            totalAmount = totalAmount,
            advanceAmount = advanceAmount,
            onSuccess = onSuccess
        )
    }

    fun advanceOrderStatus(orderId: String, currentStatus: String, newStatus: String, notes: String? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val actualDelivery = if (newStatus == "DELIVERED") now else null

            _inMemoryOrders.value = _inMemoryOrders.value.map { item ->
                if (item.order.id == orderId) {
                    val updatedOrder = item.order.copy(
                        status = newStatus,
                        actualDeliveryDate = actualDelivery ?: item.order.actualDeliveryDate,
                        updatedAt = now
                    )
                    val newHistory = OrderStatusHistoryEntity(
                        orderId = orderId,
                        fromStatus = currentStatus,
                        toStatus = newStatus,
                        notes = notes,
                        timestamp = now
                    )
                    item.copy(order = updatedOrder, statusHistory = item.statusHistory + newHistory)
                } else item
            }
            repository?.updateOrderStatus(orderId, currentStatus, newStatus, notes)
        }
    }

    fun updateOrderItemMeasurements(
        orderId: String,
        orderItemId: String,
        measurements: Map<String, Double>
    ) {
        viewModelScope.launch {
            val jsonStr = measurements.entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (k, v) ->
                "\"$k\":$v"
            }
            val currentOrders = _inMemoryOrders.value.toMutableList()
            val index = currentOrders.indexOfFirst { it.order.id == orderId }
            if (index != -1) {
                val orderDetails = currentOrders[index]
                val updatedItems = orderDetails.items.map { item ->
                    if (item.id == orderItemId) {
                        item.copy(measurementsJson = jsonStr)
                    } else item
                }
                val updatedSnapshot = if (orderDetails.items.firstOrNull()?.id == orderItemId || orderDetails.measurementSnapshot != null) {
                    orderDetails.measurementSnapshot?.copy(frozenMeasurementsJson = jsonStr)
                } else orderDetails.measurementSnapshot

                currentOrders[index] = orderDetails.copy(
                    items = updatedItems,
                    measurementSnapshot = updatedSnapshot
                )
                _inMemoryOrders.value = currentOrders
            }

            repository?.updateOrderItemMeasurements(orderId, orderItemId, measurements)
        }
    }

    fun cancelOrder(orderId: String, currentStatus: String, reason: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _inMemoryOrders.value = _inMemoryOrders.value.map { item ->
                if (item.order.id == orderId) {
                    val updatedOrder = item.order.copy(
                        status = "CANCELLED",
                        cancellationReason = reason,
                        updatedAt = now
                    )
                    val newHistory = OrderStatusHistoryEntity(
                        orderId = orderId,
                        fromStatus = currentStatus,
                        toStatus = "CANCELLED",
                        notes = "Cancelled: $reason",
                        timestamp = now
                    )
                    item.copy(order = updatedOrder, statusHistory = item.statusHistory + newHistory)
                } else item
            }
            repository?.cancelOrder(orderId, currentStatus, reason)
        }
    }
}
