package com.tailorbook.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold

/**
 * Quick Action Grid for the Dashboard (TASK-04-03 / REQ-DASH-02).
 * Enables one-tap access to New Customer, New Order, New Measurement, and Record Payment.
 */
@Composable
fun QuickActionGrid(
    onNewCustomerClick: () -> Unit,
    onNewOrderClick: () -> Unit,
    onNewMeasurementClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            label = stringResource(id = R.string.dashboard_action_new_order),
            icon = Icons.Default.ContentCut,
            modifier = Modifier.weight(1f),
            onClick = onNewOrderClick
        )

        QuickActionButton(
            label = stringResource(id = R.string.dashboard_action_new_customer),
            icon = Icons.Default.PersonAdd,
            modifier = Modifier.weight(1f),
            onClick = onNewCustomerClick
        )

        QuickActionButton(
            label = stringResource(id = R.string.dashboard_action_new_measure),
            icon = Icons.Default.Straighten,
            modifier = Modifier.weight(1f),
            onClick = onNewMeasurementClick
        )

        QuickActionButton(
            label = stringResource(id = R.string.dashboard_action_record_payment),
            icon = Icons.Default.CurrencyRupee,
            modifier = Modifier.weight(1f),
            onClick = onRecordPaymentClick
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DeepTeal.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = DeepTeal,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Charcoal,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2
        )
    }
}
