package com.tailorbook.feature.payments.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tailorbook.feature.customers.data.local.entity.CustomerEntity
import com.tailorbook.feature.customers.domain.repository.CustomerRepository
import com.tailorbook.feature.orders.data.local.entity.OrderEntity
import com.tailorbook.feature.orders.domain.repository.OrderRepository
import com.tailorbook.feature.payments.data.local.entity.PaymentEntity
import com.tailorbook.feature.payments.domain.repository.PaymentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Domain model for Customer Outstanding Balance card (TASK-10-04).
 */
data class CustomerOutstandingBalance(
    val customer: CustomerEntity,
    val ordersWithBalance: List<OrderEntity>,
    val totalBalance: Double
)

/**
 * Domain model for Payment with linked Customer & Order details (TASK-10-04).
 */
data class PaymentWithDetails(
    val payment: PaymentEntity,
    val customer: CustomerEntity?,
    val order: OrderEntity?
)

/**
 * Digital Payment Receipt Model (REQ-PAY-04 / REQ-PAY-05).
 */
data class PaymentReceiptModel(
    val receiptId: String,
    val paymentDate: Long,
    val amountPaid: Double,
    val paymentMethod: String,
    val referenceNumber: String?,
    val customerName: String,
    val customerPhone: String,
    val orderNumber: String?,
    val garmentDescription: String?,
    val totalOrderAmount: Double,
    val remainingBalance: Double,
    val notes: String?
)

/**
 * ViewModel managing Payments, Balance tracking, and Receipts (TASK-10-02 / 03 / 04).
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PaymentViewModel(
    val paymentRepository: PaymentRepository,
    val orderRepository: OrderRepository,
    val customerRepository: CustomerRepository,
    initialShopId: String = "shop-001",
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _activeShopId = MutableStateFlow(initialShopId)
    val activeShopId: StateFlow<String> = _activeShopId.asStateFlow()

    val shopId: String get() = _activeShopId.value

    fun setActiveShopId(newShopId: String) {
        val trimmed = newShopId.trim()
        if (trimmed.isNotBlank() && _activeShopId.value != trimmed) {
            _activeShopId.value = trimmed
        }
    }

    private val _selectedTab = MutableStateFlow("OUTSTANDING") // OUTSTANDING, HISTORY
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _activeReceipt = MutableStateFlow<PaymentReceiptModel?>(null)
    val activeReceipt: StateFlow<PaymentReceiptModel?> = _activeReceipt.asStateFlow()

    val allOrders: StateFlow<List<OrderEntity>> = _activeShopId
        .flatMapLatest { id -> orderRepository.getAllOrders(id) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val allCustomers: StateFlow<List<CustomerEntity>> = _activeShopId
        .flatMapLatest { id -> customerRepository.getActiveCustomers(id) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = _activeShopId
        .flatMapLatest { id -> paymentRepository.getAllPayments(id) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val totalCollected: StateFlow<Double> = _activeShopId
        .flatMapLatest { id -> paymentRepository.getTotalCollected(id) }
        .stateIn(scope, SharingStarted.Eagerly, 0.0)

    val totalOutstanding: StateFlow<Double> = allOrders.combine(allOrders) { _, orders ->
        orders.filter { it.status != "CANCELLED" }.sumOf { it.balanceAmount }
    }.stateIn(scope, SharingStarted.Eagerly, 0.0)

    /**
     * Group orders with pending balances by Customer.
     */
    val customerBalances: StateFlow<List<CustomerOutstandingBalance>> = combine(
        allOrders,
        allCustomers
    ) { orders, customers ->
        val customerMap = customers.associateBy { it.id }
        val ordersWithBalance = orders.filter { it.status != "CANCELLED" && it.balanceAmount > 0 }

        ordersWithBalance.groupBy { it.customerId }.mapNotNull { (customerId, custOrders) ->
            val customer = customerMap[customerId]
            if (customer != null) {
                CustomerOutstandingBalance(
                    customer = customer,
                    ordersWithBalance = custOrders,
                    totalBalance = custOrders.sumOf { it.balanceAmount }
                )
            } else null
        }.sortedByDescending { it.totalBalance }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Recent payment ledger entries enriched with Customer and Order numbers.
     */
    val paymentsWithDetails: StateFlow<List<PaymentWithDetails>> = combine(
        allPayments,
        allCustomers,
        allOrders
    ) { payments, customers, orders ->
        val customerMap = customers.associateBy { it.id }
        val orderMap = orders.associateBy { it.id }

        payments.filter { !it.isReversed }.map { payment ->
            PaymentWithDetails(
                payment = payment,
                customer = customerMap[payment.customerId],
                order = payment.orderId?.let { orderMap[it] }
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun setSelectedTab(tab: String) {
        _selectedTab.value = tab
    }

    fun closeReceiptDialog() {
        _activeReceipt.value = null
    }

    fun recordPayment(
        orderId: String?,
        customerId: String,
        amount: Double,
        method: String,
        referenceNumber: String?,
        notes: String?,
        onSuccess: (PaymentReceiptModel) -> Unit
    ) {
        scope.launch {
            val paymentId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val paymentEntity = PaymentEntity(
                id = paymentId,
                shopId = shopId,
                orderId = orderId,
                customerId = customerId,
                amount = amount,
                paymentMethod = method,
                referenceNumber = referenceNumber?.trim()?.ifBlank { null },
                paymentDate = now,
                notes = notes?.trim()?.ifBlank { null }
            )

            paymentRepository.recordPayment(paymentEntity)

            val order = orderId?.let { id -> allOrders.value.find { it.id == id } }
            val customer = allCustomers.value.find { it.id == customerId }

            val totalOrderAmount = order?.totalAmount ?: amount
            val remainingBalance = if (order != null) {
                (order.balanceAmount - amount).coerceAtLeast(0.0)
            } else 0.0

            val receipt = PaymentReceiptModel(
                receiptId = "RCP-%03d".format(allPayments.value.size + 1),
                paymentDate = now,
                amountPaid = amount,
                paymentMethod = method,
                referenceNumber = referenceNumber,
                customerName = customer?.name ?: "Customer",
                customerPhone = customer?.primaryPhone ?: "",
                orderNumber = order?.orderNumber,
                garmentDescription = null,
                totalOrderAmount = totalOrderAmount,
                remainingBalance = remainingBalance,
                notes = notes
            )

            _activeReceipt.value = receipt
            onSuccess(receipt)
        }
    }

    fun shareReceipt(context: Context, receipt: PaymentReceiptModel, shopName: String) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(receipt.paymentDate))

        val receiptText = buildString {
            appendLine("══════════════════════")
            appendLine("  $shopName")
            appendLine("  PAYMENT RECEIPT: #${receipt.receiptId}")
            appendLine("══════════════════════")
            appendLine("Date: $formattedDate")
            appendLine("Customer: ${receipt.customerName}")
            if (receipt.orderNumber != null) {
                appendLine("Order: #${receipt.orderNumber}")
            }
            appendLine("──────────────────────")
            appendLine("Amount Paid: ₹${receipt.amountPaid.toInt()}")
            appendLine("Payment Mode: ${receipt.paymentMethod}")
            if (!receipt.referenceNumber.isNullOrBlank()) {
                appendLine("Ref/Txn ID: ${receipt.referenceNumber}")
            }
            appendLine("──────────────────────")
            appendLine("Remaining Balance: ₹${receipt.remainingBalance.toInt()}")
            appendLine("══════════════════════")
            appendLine("Thank you for choosing $shopName!")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "$shopName Payment Receipt #${receipt.receiptId}")
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Receipt via"))
    }

    fun sendWhatsAppReminder(context: Context, balanceItem: CustomerOutstandingBalance, shopName: String) {
        val phone = balanceItem.customer.primaryPhone.replace("+", "").trim()
        val text = buildString {
            append("Hello ${balanceItem.customer.name}, ")
            append("this is a friendly reminder from $shopName regarding your tailoring orders (")
            append(balanceItem.ordersWithBalance.joinToString(", ") { "#${it.orderNumber}" })
            append("). Pending balance is ₹${balanceItem.totalBalance.toInt()}. Thank you!")
        }
        val encodedText = Uri.encode(text)
        val url = "https://api.whatsapp.com/send?phone=91$phone&text=$encodedText"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
