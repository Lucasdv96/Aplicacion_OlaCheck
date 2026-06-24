package com.tpoAppInteractivas.olacheck.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.room.util.TableInfo
import com.tpoAppInteractivas.olacheck.R
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
    //Fondo con el color primario del tema
    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ){
        // logo de la app centrado
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.LogoOlaCheck),
                contentDescription = "Logo Olacheck",
                modifier = Modifier.size(200.dp)
            )
            Text(
                text = "Bienvenido a OlaCheck",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        // LOGO DE DEVOLPER ABAJO CENTRADO
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Desarrollado por Lucas Del Valle",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha =  0.7f)
            )
            Image(
                painter = painterResource(id = R.drawable.LogoLDV),
                contentDescription = "Logo UTN",
                modifier = Modifier.size(100.dp)
            )

        }
    }
}
