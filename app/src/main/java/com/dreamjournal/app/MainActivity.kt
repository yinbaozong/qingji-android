package com.dreamjournal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dreamjournal.app.ui.navigation.DreamJournalApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as DreamJournalApplication).container
        setContent {
            DreamJournalApp(container = appContainer)
        }
    }
}
