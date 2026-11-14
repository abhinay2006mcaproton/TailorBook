package com.tailorbook.feature.payments.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory

/**
 * Record Payment Screen (TASK-10-03 / REQ-PAY-01 / REQ-PAY-02).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    viewModel: PaymentViewModel,
    initialOrderId: String? = null,
    initialCustomerId: String? = null,
    onNavigateBack: () -> Unit,
    onPaymentRecorded: (PaymentReceiptModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCustomers by viewModel.allCustomers.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()

    var selectedCustomerId by remember {
        mutableStateOf(initialCustomerId ?: allCustomers.firstOrNull()?.id ?: "")
    }

    LaunchedEffect(allCustomers) {
        if (selectedCustomerId.isBlank() && allCustomers.isNotEmpty()) {
            selectedCustomerId = initialCustomerId ?: allCustomers.first().id
        }
    }

    val customerOrders = remember(selectedCustomerId, allOrders) {
        allOrders.filter { it.customerId == selectedCustomerId && it.status != "CANCELLED" }
    }

    var selectedOrderId by remember {
        mutableStateOf(initialOrderId ?: customerOrders.firstOrNull { it.balanceAmount > 0 }?.id ?: customerOrders.firstOrNull()?.id)
    }

    LaunchedEffect(customerOrders) {
        if (selectedOrderId == null || customerOrders.none { it.id == selectedOrderId }) {
            selectedOrderId = customerOrders.firstOrNull { it.balanceAmount > 0 }?.id ?: customerOrders.firstOrNull()?.id
        }
    }

    val selectedOrder = remember(selectedOrderId, customerOrders) {
        customerOrders.find { it.id == selectedOrderId }
    }

    var amountText by remember {
        mutableStateOf(selectedOrder?.balanceAmount?.toInt()?.toString() ?: "500")
    }

    // Auto update amount when order changes
    LaunchedEffect(selectedOrder) {
        if (selectedOrder != null && selectedOrder.balanceAmount > 0) {
            amountText = selectedOrder.balanceAmount.toInt().toString()
        }
    }

    var selectedPaymentMethod by remember { mutableStateOf("CASH") } // CASH, UPI, BANK_TRANSFER, CHEQUE
    var referenceNumberText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.payments_record_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CardWhite
                        )
                        Text(
                            text = stringResource(R.string.payments_record_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftGold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CardWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepTeal)
            )
        },
        bottomBar = {
            Surface(
                color = CardWhite,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    val amountValue = amountText.toDoubleOrNull() ?: 0.0
                    TailorPrimaryButton(
                        text = "Record ₹${amountValue.toInt()} & Generate Receipt",
                        onClick = {
                            if (amountValue > 0 && selectedCustomerId.isNotBlank()) {
                                viewModel.recordPayment(
                                    orderId = selectedOrderId,
                                    customerId = selectedCustomerId,
                                    amount = amountValue,
                                    method = selectedPaymentMethod,
                                    referenceNumber = referenceNumberText,
                                    notes = notesText,
                                    onSuccess = onPaymentRecorded
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        containerColor = WarmIvory,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Section 1: Customer Selector
            TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. Select Customer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allCustomers) { cust ->
                            val isSelected = cust.id == selectedCustomerId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCustomerId = cust.id },
                                label = { Text(cust.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepTeal,
                                    selectedLabelColor = CardWhite,
                                    selectedLeadingIconColor = CardWhite,
                                    containerColor = CardWhite,
                                    labelColor = Charcoal
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 2: Order Linkage (if customer has orders)
            if (customerOrders.isNotEmpty()) {
                TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "2. Select Order (Optional)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepTeal
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        customerOrders.forEach { order ->
                            val isSelected = order.id == selectedOrderId
                            Surface(
                                color = if (isSelected) DeepTeal.copy(alpha = 0.1f) else LightBeige.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedOrderId = order.id }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Order #${order.orderNumber}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Charcoal
                                        )
                                        Text(
                                            text = "Total: ₹${order.totalAmount.toInt()} • Paid: ₹${order.paidAmount.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CharcoalSubtle
                                        )
                                    }

                                    Text(
                                        text = "Due: ₹${order.balanceAmount.toInt()}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (order.balanceAmount > 0) StatusRed else StatusGreen
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Section 3: Amount & Quick Shortcuts
            TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "3. Payment Amount",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                        label = { Text("Amount (₹)") },
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = DeepTeal) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = LightBeige
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shortcut buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (selectedOrder != null && selectedOrder.balanceAmount > 0) {
                            AssistChip(
                                onClick = { amountText = selectedOrder.balanceAmount.toInt().toString() },
                                label = { Text(stringResource(R.string.payments_full_balance), fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = CardWhite)
                            )
                        }
                        AssistChip(
                            onClick = { amountText = "500" },
                            label = { Text("₹500", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardWhite)
                        )
                        AssistChip(
                            onClick = { amountText = "1000" },
                            label = { Text("₹1000", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardWhite)
                        )
                        AssistChip(
                            onClick = { amountText = "2000" },
                            label = { Text("₹2000", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = CardWhite)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 4: Payment Method
            TailorElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "4. Payment Mode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepTeal
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val modes = listOf(
                        "CASH" to stringResource(R.string.payments_method_cash),
                        "UPI" to stringResource(R.string.payments_method_upi),
                        "BANK" to stringResource(R.string.payments_method_bank),
                        "CHEQUE" to stringResource(R.string.payments_method_cheque)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        modes.forEach { (mode, label) ->
                            val isSelected = selectedPaymentMethod == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPaymentMethod = mode },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepTeal,
                                    selectedLabelColor = CardWhite,
                                    containerColor = CardWhite,
                                    labelColor = Charcoal
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reference # text field
                    OutlinedTextField(
                        value = referenceNumberText,
                        onValueChange = { referenceNumberText = it },
                        label = { Text(stringResource(R.string.payments_ref_number)) },
                        placeholder = { Text("e.g. UPI Ref / UTR / Cheque #") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = LightBeige
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notes text field
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text(stringResource(R.string.payments_notes)) },
                        placeholder = { Text("e.g. Final balance received on delivery") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepTeal,
                            unfocusedBorderColor = LightBeige
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
