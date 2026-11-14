package com.tailorbook.feature.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import com.tailorbook.ui.components.TailorPrimaryButton
import com.tailorbook.ui.theme.Charcoal
import com.tailorbook.ui.theme.CharcoalSubtle
import com.tailorbook.ui.theme.DeepTeal
import com.tailorbook.ui.theme.MutedSage
import com.tailorbook.ui.theme.SoftGold
import com.tailorbook.ui.theme.StatusAmber
import com.tailorbook.ui.theme.StatusGreen
import com.tailorbook.ui.theme.StatusRed
import com.tailorbook.ui.theme.WarmIvory

/**
 * Enterprise Production Login Screen.
 * Provider-independent architecture:
 * Tab 0: Email + Password (live with Supabase GoTrue Auth) + 6-digit Email OTP recovery.
 * Tab 1: Phone OTP (displays standardized notice: "Phone verification is not available yet. Please use email login for now.")
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    initialEmail: String = "",
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var showForgotDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = WarmIvory,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedTabIndex == 0) {
                    TailorPrimaryButton(
                        text = stringResource(id = R.string.auth_btn_login),
                        onClick = {
                            focusManager.clearFocus()
                            authViewModel.signInWithEmail(email, password)
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        text = stringResource(id = R.string.auth_link_need_account),
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
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Logo & Branding
            Image(
                painter = painterResource(id = R.drawable.ic_tailorbook_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DeepTeal,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = stringResource(id = R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CharcoalSubtle,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Auth Method Selector Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = WarmIvory,
                contentColor = DeepTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = DeepTeal,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        authViewModel.clearMessages()
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.auth_tab_email),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        authViewModel.clearMessages()
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.auth_tab_phone),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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

            // Success Banner
            if (!uiState.successMessage.isNullOrBlank()) {
                TailorElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusGreen.copy(alpha = 0.12f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.successMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StatusGreen,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Tab 0: Email + Password Form
            if (selectedTabIndex == 0) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        authViewModel.clearMessages()
                    },
                    label = { Text(stringResource(id = R.string.auth_label_email)) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = DeepTeal)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepTeal,
                        unfocusedBorderColor = MutedSage,
                        focusedLabelColor = DeepTeal,
                        cursorColor = DeepTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        authViewModel.clearMessages()
                    },
                    label = { Text(stringResource(id = R.string.auth_label_password)) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = DeepTeal)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = CharcoalSubtle
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            authViewModel.signInWithEmail(email, password)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepTeal,
                        unfocusedBorderColor = MutedSage,
                        focusedLabelColor = DeepTeal,
                        cursorColor = DeepTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showForgotDialog = true }) {
                        Text(
                            text = stringResource(id = R.string.auth_link_forgot_password),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DeepTeal,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                if (uiState.isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = DeepTeal, modifier = Modifier.size(32.dp))
                }
            } else {
                // Tab 1: Phone OTP Notice (Section 5 Requirement)
                TailorElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusAmber.copy(alpha = 0.08f))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(StatusAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(id = R.string.auth_phone_unavailable_notice),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Charcoal,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TailorPrimaryButton(
                            text = "Use Email Login",
                            onClick = { selectedTabIndex = 0 }
                        )
                    }
                }
            }
        }
    }

    // 6-Digit Email OTP Forgot Password Dialog
    if (showForgotDialog) {
        ForgotPasswordDialog(
            authViewModel = authViewModel,
            onDismiss = {
                showForgotDialog = false
                authViewModel.clearMessages()
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var resetEmail by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isNewPassVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.auth_forgot_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DeepTeal
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.auth_forgot_desc),
                    style = MaterialTheme.typography.bodySmall.copy(color = CharcoalSubtle)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text(stringResource(id = R.string.auth_label_email)) },
                    singleLine = true,
                    enabled = !uiState.isPasswordResetRequested,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.isPasswordResetRequested) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        label = { Text(stringResource(id = R.string.auth_label_otp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(id = R.string.auth_label_new_password)) },
                        singleLine = true,
                        visualTransformation = if (isNewPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isNewPassVisible = !isNewPassVisible }) {
                                Icon(
                                    imageVector = if (isNewPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(color = StatusRed)
                    )
                }

                if (!uiState.successMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.successMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(color = StatusGreen)
                    )
                }
            }
        },
        confirmButton = {
            if (!uiState.isPasswordResetRequested) {
                TextButton(
                    onClick = { authViewModel.requestPasswordResetOtp(resetEmail) },
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = stringResource(id = R.string.auth_forgot_btn_send_otp),
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                }
            } else {
                TextButton(
                    onClick = {
                        authViewModel.resetPasswordWithOtp(resetEmail, otpCode, newPassword)
                    },
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = stringResource(id = R.string.auth_btn_reset_password),
                        fontWeight = FontWeight.Bold,
                        color = DeepTeal
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel), color = CharcoalSubtle)
            }
        },
        containerColor = WarmIvory,
        shape = RoundedCornerShape(16.dp)
    )
}
