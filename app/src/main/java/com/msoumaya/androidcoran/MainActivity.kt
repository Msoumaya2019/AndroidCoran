package com.msoumaya.androidcoran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.msoumaya.androidcoran.ui.CoranApp

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState);enableEdgeToEdge();setContent { CoranApp() } }
}
