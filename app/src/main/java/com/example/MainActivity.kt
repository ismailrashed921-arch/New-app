package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.RiderAppUi
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.RiderViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: RiderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Notification system
        NotificationHelper.createNotificationChannel(applicationContext)

        // Request Notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
        
        // Auto-login check
        val prefs = getSharedPreferences("RiderPrefs", Context.MODE_PRIVATE)
        val savedPhone = prefs.getString("riderPhone", null)
        if (!savedPhone.isNullOrEmpty()) {
            viewModel.startApp(applicationContext, savedPhone)
        }

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RiderAppUi(viewModel = viewModel)
                }
            }
        }
    }
}
