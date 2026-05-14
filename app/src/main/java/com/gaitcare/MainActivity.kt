package com.gaitcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gaitcare.ui.navigation.GaitCareNavHost
import com.gaitcare.ui.theme.GaitCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaitCareTheme {
                GaitCareNavHost()
            }
        }
    }
}
