package com.tailorbook.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.TailorBookTheme
import com.tailorbook.ui.theme.WarmIvory

// ─────────────────────────────────────────────────────────────────────────────
// TailorTextField — Outlined text field variants
//
// Design spec:
//   - OutlinedTextField shape: 12dp corner radius
//   - Unfocused border: Muted Sage (#84978A) at 1dp
//   - Focused border: Deep Teal (#203B36) at 2dp
//   - Background: Warm Ivory (#F8F5EF)
//   - Error border: Status Red
//   - Label: floats above on focus/non-empty
//   - Supporting text: error message or helper hint below the field
//
// Measurement / money fields:
//   - KeyboardType.Decimal for numeric entry
//   - Placeholder shows the input format hint e.g. "14.5 or 14½"
//   - Unit suffix shown as trailing text (e.g., "inches", "cm", "₹")
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Standard single-line outlined text field.
 *
 * @param value          Current text value (controlled).
 * @param onValueChange  Called whenever the user changes the text.
 * @param label          Floating label shown above the field when focused.
 * @param modifier       Layout modifier.
 * @param placeholder    Greyed-out hint shown when [value] is empty and field is focused.
 * @param supportingText Helper or error text below the field; shown in red when [isError]=true.
 * @param isError        When true, the border and supportingText turn Status Red.
 * @param enabled        Whether the field accepts input.
 * @param singleLine     Enforces single-line input (default true).
 * @param maxLines       Maximum lines when [singleLine]=false.
 * @param leadingIcon    Optional icon on the left inside the field.
 * @param trailingIcon   Optional icon on the right inside the field.
 * @param keyboardType   Keyboard type (default [KeyboardType.Text]).
 * @param imeAction      IME action button (default [ImeAction.Next]).
 * @param keyboardActions Callbacks for IME action events.
 */
@Composable
fun TailorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder, color = CharcoalSubtle.copy(alpha = 0.6f)) }
        } else null,
        supportingText = if (supportingText != null) {
            {
                Text(
                    text = supportingText,
                    color = if (isError) StatusRed else CharcoalSubtle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else null,
        isError = isError,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else maxLines,
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null, tint = if (isError) StatusRed else MutedSage) }
        } else null,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = DeepTeal,
            unfocusedBorderColor = MutedSage,
            errorBorderColor     = StatusRed,
            focusedLabelColor    = DeepTeal,
            unfocusedLabelColor  = CharcoalSubtle,
            errorLabelColor      = StatusRed,
            cursorColor          = DeepTeal,
            focusedContainerColor   = WarmIvory,
            unfocusedContainerColor = WarmIvory,
            disabledContainerColor  = LightBeige
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction    = imeAction
        ),
        keyboardActions = keyboardActions
    )
}

/**
 * Measurement input field.
 *
 * Configured for decimal numeric input with an inline unit suffix (e.g., "in", "cm").
 * Shows a placeholder hint for the accepted format ("e.g. 14.5 or 14.25").
 *
 * @param value      Current measurement value as a string (e.g. "14.5").
 * @param onValueChange Callback with the raw string the user typed.
 * @param label      Field label (e.g., "Chest", "Waist").
 * @param unit       Unit displayed as a trailing suffix text (e.g., "in", "cm").
 * @param modifier   Layout modifier.
 * @param supportingText Optional helper / error text.
 * @param isError    Whether the input is in an error state.
 */
@Composable
fun TailorMeasurementField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String = "in",
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    TailorTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = "e.g. 14.5",
        supportingText = supportingText,
        isError = isError,
        enabled = enabled,
        keyboardType = KeyboardType.Decimal,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        trailingIcon = {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelMedium,
                color = CharcoalSubtle,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    )
}

/**
 * Currency / money input field.
 *
 * Shows a "₹" prefix icon and uses decimal keyboard.
 * Use for price, advance, and outstanding amount inputs.
 *
 * @param value         Current value as a string (e.g. "1500.00").
 * @param onValueChange Callback with the raw string.
 * @param label         Field label (e.g., "Price", "Advance Paid").
 */
@Composable
fun TailorMoneyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    TailorTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = "0.00",
        supportingText = supportingText,
        isError = isError,
        enabled = enabled,
        keyboardType = KeyboardType.Decimal,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        leadingIcon = null,  // ₹ is shown as trailingIcon start position via prefix
        trailingIcon = null
        // We rely on the label and placeholder for the ₹ context; an explicit
        // prefix "₹" Text composable would need a BasicTextField — keeping it
        // simple with OutlinedTextField here and relying on label to carry the unit.
    )
}

/**
 * Phone number field.
 *
 * Numeric keyboard, max 10 digits enforced by caller, placeholder shows format.
 */
@Composable
fun TailorPhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Mobile Number",
    modifier: Modifier = Modifier,
    placeholder: String = "10-digit mobile number",
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    TailorTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        supportingText = supportingText,
        isError = isError,
        enabled = enabled,
        keyboardType = KeyboardType.Phone,
        imeAction = imeAction,
        keyboardActions = keyboardActions
    )
}

/**
 * Password field with a show/hide toggle.
 *
 * Uses [PasswordVisualTransformation] by default and toggles to plain text when
 * the eye icon is tapped.
 */
@Composable
fun TailorPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Password",
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = if (supportingText != null) {
            {
                Text(
                    text = supportingText,
                    color = if (isError) StatusRed else CharcoalSubtle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else null,
        isError = isError,
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None
                               else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility
                                  else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = MutedSage
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = DeepTeal,
            unfocusedBorderColor = MutedSage,
            errorBorderColor     = StatusRed,
            focusedLabelColor    = DeepTeal,
            unfocusedLabelColor  = CharcoalSubtle,
            cursorColor          = DeepTeal,
            focusedContainerColor   = WarmIvory,
            unfocusedContainerColor = WarmIvory
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction    = imeAction
        ),
        keyboardActions = keyboardActions
    )
}

/**
 * Multi-line notes / instructions text area.
 *
 * Used for stitching instructions, customer notes, order-specific notes.
 *
 * @param minLines Minimum visible lines (default 3).
 * @param maxLines Maximum lines before scroll (default 8).
 */
@Composable
fun TailorNotesField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Notes",
    modifier: Modifier = Modifier,
    placeholder: String = "Add any additional notes or instructions...",
    minLines: Int = 3,
    maxLines: Int = 8,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = CharcoalSubtle.copy(alpha = 0.6f)) },
        supportingText = if (supportingText != null) {
            { Text(supportingText, color = if (isError) StatusRed else CharcoalSubtle) }
        } else null,
        isError = isError,
        enabled = enabled,
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = DeepTeal,
            unfocusedBorderColor = MutedSage,
            errorBorderColor     = StatusRed,
            focusedLabelColor    = DeepTeal,
            unfocusedLabelColor  = CharcoalSubtle,
            cursorColor          = DeepTeal,
            focusedContainerColor   = WarmIvory,
            unfocusedContainerColor = WarmIvory
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction    = ImeAction.Default
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorTextFieldPreview() {
    TailorBookTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TailorTextField(value = "Priya Reddy", onValueChange = {}, label = "Customer Name")
            Spacer(modifier = Modifier.height(16.dp))
            TailorTextField(
                value = "",
                onValueChange = {},
                label = "Customer Name",
                placeholder = "Enter full name",
                supportingText = "Name is required",
                isError = true
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorMeasurementFieldPreview() {
    TailorBookTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TailorMeasurementField(value = "36.5", onValueChange = {}, label = "Bust / Chest", unit = "in")
            Spacer(modifier = Modifier.height(16.dp))
            TailorMeasurementField(value = "", onValueChange = {}, label = "Waist", unit = "in", isError = true, supportingText = "Please enter waist measurement")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F5EF)
@Composable
private fun TailorNotesFieldPreview() {
    TailorBookTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TailorNotesField(
                value = "Please use matching lining. Customer prefers full sleeve.",
                onValueChange = {},
                label = "Stitching Instructions"
            )
        }
    }
}
