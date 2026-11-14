package com.tailorbook.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tailorbook.R
import com.tailorbook.ui.components.TailorElevatedCard
import com.tailorbook.ui.components.TailorPhoneField
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.components.TailorTextField
import com.tailorbook.ui.theme.CardWhite
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.LightBeige
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory

data class BusinessTypeOption(
    val id: String,
    val titleRes: Int,
    val icon: ImageVector
)

val businessTypeOptions = listOf(
    BusinessTypeOption("women", R.string.shop_reg_type_women, Icons.Default.Checkroom),
    BusinessTypeOption("men", R.string.shop_reg_type_men, Icons.Default.Man),
    BusinessTypeOption("family", R.string.shop_reg_type_family, Icons.Default.FamilyRestroom),
    BusinessTypeOption("designer", R.string.shop_reg_type_designer, Icons.Default.Diamond)
)

/**
 * Enterprise Production Shop Registration Screen.
 * Fully wired with Supabase GoTrue Auth for real cloud account creation.
 */
@Composable
fun ShopRegistrationScreen(
    authViewModel: AuthViewModel,
    onRegistrationComplete: (shopName: String, ownerName: String, phone: String, businessType: String, email: String, upiId: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var shopName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var selectedBusinessType by remember { mutableStateOf("women") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isRegistrationSuccessful) {
        if (uiState.isRegistrationSuccessful) {
            showSuccessDialog = true
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToLogin) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.action_back),
                        tint = DeepTeal
                    )
                }
                Text(
                    text = stringResource(id = R.string.auth_link_already_have_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepTeal
                )
            }
        },
        containerColor = WarmIvory,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TailorPrimaryButton(
                    text = stringResource(id = R.string.auth_btn_register),
                    onClick = {
                        focusManager.clearFocus()
                        authViewModel.signUpWithEmail(
                            email = email,
                            password = password,
                            confirmPass = confirmPassword,
                            shopName = shopName,
                            ownerName = ownerName,
                            phone = phone,
                            upiId = upiId
                        )
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = stringResource(id = R.string.auth_link_already_have_account),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = DeepTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.ic_tailorbook_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.shop_reg_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = DeepTeal
                )
            )

            Text(
                text = stringResource(id = R.string.shop_reg_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CharcoalSubtle,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Error Banner
            if (!uiState.errorMessage.isNullOrBlank()) {
                TailorElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusRed.copy(alpha = 0.12f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StatusRed,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Input Fields
            TailorTextField(
                value = shopName,
                onValueChange = {
                    shopName = it
                    authViewModel.clearMessages()
                },
                label = stringResource(id = R.string.shop_reg_field_shop_name),
                leadingIcon = Icons.Default.Storefront,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            TailorTextField(
                value = ownerName,
                onValueChange = {
                    ownerName = it
                    authViewModel.clearMessages()
                },
                label = stringResource(id = R.string.shop_reg_field_owner_name),
                leadingIcon = Icons.Default.Person,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    authViewModel.clearMessages()
                },
                label = { Text(stringResource(id = R.string.auth_label_email)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = DeepTeal) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = MutedSage,
                    focusedLabelColor = DeepTeal,
                    cursorColor = DeepTeal
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    authViewModel.clearMessages()
                },
                label = { Text(stringResource(id = R.string.auth_label_password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DeepTeal) },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = MutedSage,
                    focusedLabelColor = DeepTeal,
                    cursorColor = DeepTeal
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    authViewModel.clearMessages()
                },
                label = { Text(stringResource(id = R.string.auth_label_confirm_password)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DeepTeal) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepTeal,
                    unfocusedBorderColor = MutedSage,
                    focusedLabelColor = DeepTeal,
                    cursorColor = DeepTeal
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            TailorPhoneField(
                value = phone,
                onValueChange = {
                    phone = it
                    authViewModel.clearMessages()
                },
                label = stringResource(id = R.string.shop_reg_field_phone),
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            TailorTextField(
                value = upiId,
                onValueChange = {
                    upiId = it.trim()
                    authViewModel.clearMessages()
                },
                label = "Boutique UPI ID (for QR Payments)",
                leadingIcon = Icons.Default.QrCode,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Business Type Section
            Text(
                text = stringResource(id = R.string.shop_reg_field_business_type),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DeepTeal
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            businessTypeOptions.forEach { option ->
                val isSelected = selectedBusinessType == option.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedBusinessType = option.id },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) LightBeige else CardWhite),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) DeepTeal else MutedSage
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) DeepTeal else CharcoalSubtle,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(id = option.titleRes),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) DeepTeal else Charcoal
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = if (isSelected) DeepTeal else MutedSage,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = DeepTeal, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Success / Confirmation Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* forced action */ },
            title = {
                Text(
                    text = "Confirm Your Email ✉️",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "We have sent an activation link to:\n$email",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = DeepTeal
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "📱 Please open your email on THIS phone and tap the confirmation link. It will automatically redirect and activate TailorBook!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Charcoal,
                            lineHeight = 18.sp
                        )
                    )
                }
            },
            confirmButton = {
                TailorPrimaryButton(
                    text = "Got It, Open Email",
                    onClick = {
                        showSuccessDialog = false
                        authViewModel.clearMessages()
                        // Save profile draft
                        onRegistrationComplete(shopName, ownerName, phone, selectedBusinessType, email, upiId)
                    }
                )
            },
            containerColor = WarmIvory,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
