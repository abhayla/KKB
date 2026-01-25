package com.rasoiai.app.presentation.auth

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.R
import com.rasoiai.app.presentation.splash.components.AppLogo
import com.rasoiai.app.presentation.theme.spacing

@Composable
fun AuthScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigationEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            AuthNavigationEvent.NavigateToOnboarding -> {
                onNavigateToOnboarding()
                viewModel.onNavigationHandled()
            }
            AuthNavigationEvent.NavigateToHome -> {
                onNavigateToHome()
                viewModel.onNavigationHandled()
            }
            null -> { /* No navigation */ }
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
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(spacing.xxl))

            // Welcome text
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            // Tagline
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(0.2f))

            // Google Sign-In Button
            GoogleSignInButton(
                onClick = {
                    // Need Activity context for Credential Manager
                    (context as? Activity)?.let { activity ->
                        viewModel.signInWithGoogle(activity)
                    }
                },
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(0.15f))

            // Terms and Privacy
            TermsAndPrivacyText(
                onTermsClick = { /* TODO: Open Terms URL */ },
                onPrivacyClick = { /* TODO: Open Privacy URL */ },
                modifier = Modifier.padding(bottom = spacing.xl)
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
private fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(spacing.sm),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.7f),
            disabledContentColor = Color.Black.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(spacing.md))
            Text(
                text = stringResource(R.string.signing_in),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        } else {
            // Google "G" icon
            GoogleIcon(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(spacing.md))
            Text(
                text = stringResource(R.string.sign_in_with_google),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GoogleIcon(modifier: Modifier = Modifier) {
    // Using a simple "G" text styled like Google's logo
    // In production, use the official Google sign-in button asset
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4285F4) // Google Blue
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
            text = stringResource(R.string.auth_terms_prefix),
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
                    text = stringResource(R.string.auth_terms_of_service),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            }
            Text(
                text = " • ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onPrivacyClick) {
                Text(
                    text = stringResource(R.string.auth_privacy_policy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

// region Previews

@androidx.compose.ui.tooling.preview.Preview(
    name = "Light Mode",
    showBackground = true,
    backgroundColor = 0xFFFDFAF4
)
@androidx.compose.ui.tooling.preview.Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun AuthScreenPreview() {
    com.rasoiai.app.presentation.theme.RasoiAITheme {
        AuthScreenContent(isLoading = false)
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Loading State",
    showBackground = true,
    backgroundColor = 0xFFFDFAF4
)
@Composable
private fun AuthScreenLoadingPreview() {
    com.rasoiai.app.presentation.theme.RasoiAITheme {
        AuthScreenContent(isLoading = true)
    }
}

@Composable
private fun AuthScreenContent(isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            AppLogo(modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(spacing.lg))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(spacing.xxl))

            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(spacing.sm))

            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(0.2f))

            GoogleSignInButton(
                onClick = { },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(0.15f))

            TermsAndPrivacyText(
                onTermsClick = { },
                onPrivacyClick = { },
                modifier = Modifier.padding(bottom = spacing.xl)
            )
        }
    }
}

// endregion
