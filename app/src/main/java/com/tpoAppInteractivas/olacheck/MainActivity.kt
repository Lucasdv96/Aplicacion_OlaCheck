package com.tpoAppInteractivas.olacheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tpoAppInteractivas.olacheck.ui.navigation.NavGraph
import com.tpoAppInteractivas.olacheck.ui.theme.OlaCheckTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OlaCheckTheme {
                NavGraph()
            }
        }
    }
}