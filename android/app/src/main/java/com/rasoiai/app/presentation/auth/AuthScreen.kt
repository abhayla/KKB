package com.rasoiai.app.presentation.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.R
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.splash.components.AppLogo
import com.rasoiai.app.presentation.theme.spacing

@Composable
fun AuthScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                AuthNavigationEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                AuthNavigationEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(TestTags.AUTH_SCREEN)
    ) {
        if (uiState.otpSent) {
            OtpVerificationContent(
                uiState = uiState,
                onOtpCodeChanged = viewModel::updateOtpCode,
                onVerifyOtp = viewModel::verifyOtp,
                onResendOtp = {
                    (context as? Activity)?.let { activity ->
                        viewModel.resendOtp(activity)
                    }
                },
                onGoBack = viewModel::goBack
            )
        } else {
            PhoneInputContent(
                uiState = uiState,
                onPhoneNumberChanged = viewModel::updatePhoneNumber,
                onSendOtp = {
                    (context as? Activity)?.let { activity ->
                        viewModel.sendOtp(activity)
                    }
                },
                onTermsClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rasoiai.com/terms"))
                    context.startActivity(intent)
                },
                onPrivacyClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rasoiai.com/privacy"))
                    context.startActivity(intent)
                }
            )
        }

        // Snackbar for errors
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { snackbarData ->
            Snackbar(
                snackbarData = snackbarData,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun PhoneInputContent(
    uiState: AuthUiState,
    onPhoneNumberChanged: (String) -> Unit,
    onSendOtp: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        // Logo section
        AppLogo(modifier = Modifier.size(100.dp))

        Spacer(modifier = Modifier.height(spacing.lg))

        // App name
        Text(
            text = "RasoiAI",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        // Welcome text
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag(TestTags.AUTH_WELCOME_TEXT)
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        // Tagline
        Text(
            text = "AI Meal Planning for Indian Families",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(0.15f))

        // Phone number input
        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = onPhoneNumberChanged,
            label = { Text("Phone Number") },
            placeholder = { Text("Enter 10-digit number") },
            prefix = {
                Text(
                    text = "+91 ",
                    modifier = Modifier.testTag(TestTags.COUNTRY_CODE_PREFIX)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.PHONE_NUMBER_FIELD)
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        // Send OTP Button
        Button(
            onClick = onSendOtp,
            enabled = uiState.isPhoneValid && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag(TestTags.SEND_OTP_BUTTON),
            shape = RoundedCornerShape(spacing.sm)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(spacing.md))
                Text(
                    text = "Sending OTP...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Send OTP",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.15f))

        // Terms and Privacy
        TermsAndPrivacyText(
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick,
            modifier = Modifier.padding(bottom = spacing.xl)
        )
    }
}

@Composable
private fun OtpVerificationContent(
    uiState: AuthUiState,
    onOtpCodeChanged: (String) -> Unit,
    onVerifyOtp: () -> Unit,
    onResendOtp: () -> Unit,
    onGoBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(spacing.xl))

        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = onGoBack,
                modifier = Modifier.testTag(TestTags.AUTH_BACK_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back"
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Title
        Text(
            text = "Verify your number",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag(TestTags.OTP_SCREEN_TITLE)
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        // Phone display
        Text(
            text = "OTP sent to +91 ${uiState.phoneNumber}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TestTags.OTP_PHONE_DISPLAY)
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        // OTP Input boxes
        OtpInputRow(
            otpCode = uiState.otpCode,
            onOtpCodeChanged = onOtpCodeChanged
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        // Verify button
        Button(
            onClick = onVerifyOtp,
            enabled = uiState.otpCode.length == 6 && !uiState.isVerifying,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag(TestTags.VERIFY_OTP_BUTTON),
            shape = RoundedCornerShape(spacing.sm)
        ) {
            if (uiState.isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(spacing.md))
                Text(
                    text = "Verifying...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Verify OTP",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        // Resend OTP
        TextButton(
            onClick = onResendOtp,
            enabled = uiState.resendCountdownSeconds == 0 && !uiState.isLoading,
            modifier = Modifier.testTag(TestTags.RESEND_OTP_BUTTON)
        ) {
            Text(
                text = if (uiState.resendCountdownSeconds > 0) {
                    "Resend OTP in ${uiState.resendCountdownSeconds}s"
                } else {
                    "Resend OTP"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.resendCountdownSeconds > 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
private fun OtpInputRow(
    otpCode: String,
    onOtpCodeChanged: (String) -> Unit
) {
    // Hidden text field that captures keyboard input
    Box {
        BasicTextField(
            value = otpCode,
            onValueChange = { value ->
                if (value.length <= 6 && value.all { it.isDigit() }) {
                    onOtpCodeChanged(value)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            decorationBox = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val char = otpCode.getOrNull(index)?.toString() ?: ""
                        val isFocused = index == otpCode.length

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .testTag("${TestTags.OTP_INPUT_PREFIX}$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun TermsAndPrivacyText(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "By continuing, you agree to our",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onTermsClick) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            }
            Text(
                text = " \u2022 ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onPrivacyClick) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}
