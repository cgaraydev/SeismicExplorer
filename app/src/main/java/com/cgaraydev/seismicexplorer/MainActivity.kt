package com.cgaraydev.seismicexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cgaraydev.seismicexplorer.presentation.navigation.SeismicNavHost
import com.cgaraydev.seismicexplorer.ui.theme.SeismicExplorerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SeismicExplorerTheme {
                SeismicNavHost()
            }
        }
    }
}