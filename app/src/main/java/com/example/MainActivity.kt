package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.navigation.WidgetLiteApp
import com.example.ui.viewmodel.MainViewModel
import com.example.widget.WidgetUpdateHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialConfigId = intent?.getLongExtra(WidgetUpdateHelper.EXTRA_CONFIG_ID, -1L) ?: -1L

        setContent {
            WidgetLiteApp(
                viewModel = viewModel,
                initialConfigId = initialConfigId
            )
        }
    }
}
