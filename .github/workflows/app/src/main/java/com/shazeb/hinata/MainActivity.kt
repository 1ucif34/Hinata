package com.shazeb.hinata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.shazeb.hinata.ui.screens.ChatScreen
import com.shazeb.hinata.ui.theme.HinataTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HinataTheme {
                ChatScreen()
            }
        }
    }
}
