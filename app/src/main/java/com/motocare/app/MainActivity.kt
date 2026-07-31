package com.motocare.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.motocare.app.ui.MotoCareApp
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationDestination.value = intent.getStringExtra(EXTRA_DESTINATION)
        enableEdgeToEdge()
        setContent {
            MotoCareApp(
                notificationDestination = notificationDestination.value,
                onNotificationDestinationHandled = { notificationDestination.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationDestination.value = intent.getStringExtra(EXTRA_DESTINATION)
    }

    companion object {
        const val EXTRA_DESTINATION = "motocare_destination"
    }
}
