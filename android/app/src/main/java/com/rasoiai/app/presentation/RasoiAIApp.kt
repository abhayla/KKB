package com.rasoiai.app.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.rasoiai.app.presentation.navigation.RasoiNavHost

@Composable
fun RasoiAIApp() {
    val navController = rememberNavController()
    RasoiNavHost(navController = navController)
}
