package com.tpoAppInteractivas.olacheck.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpoAppInteractivas.olacheck.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    onNavigateToHome : () -> Unit,
    onNavigateToLogin : () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()

){
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()

    LaunchedEffect(isUserLoggedIn){
        when(isUserLoggedIn){
            true -> onNavigateToHome()
            false -> onNavigateToLogin()
            null -> Unit
        }
    }
    Box (
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Text(text = "OlaCheck", fontSize = 32.sp)
    }


}
