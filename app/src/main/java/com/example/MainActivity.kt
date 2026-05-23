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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.RiderAppUi
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.RiderViewModel
import android.content.Intent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: RiderViewModel by viewModels()

    companion object {
        var isAppInForeground = false
    }

    override fun onStart() {
        super.onStart()
        isAppInForeground = true
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val prefs = getSharedPreferences("RiderPrefs", Context.MODE_PRIVATE)

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
        
        // Observe online state to start/stop the persistent RiderService
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isOnline.collect { online ->
                    val serviceIntent = Intent(applicationContext, RiderService::class.java)
                    if (online) {
                        val savedPhone = prefs.getString("riderPhone", null)
                        if (!savedPhone.isNullOrEmpty()) {
                            serviceIntent.putExtra("riderPhone", savedPhone)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    } else {
                        stopService(serviceIntent)
                    }
                }
            }
        }
        
        // Auto-login check
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
