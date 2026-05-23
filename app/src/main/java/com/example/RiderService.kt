package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.viewmodel.toOrder
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RiderService : Service() {

    private val CHANNEL_ID = "kilagbe_service_channel"
    private val NOTIFICATION_ID = 1002

    private var db: FirebaseFirestore? = null
    private var orderListener: ListenerRegistration? = null
    private var currentRiderPhone: String? = null
    private val notifiedOrderIds = HashSet<String>()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rider Service Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that you are online and ready for orders"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        initFirebase()
    }

    private fun initFirebase() {
        if (db == null) {
            try {
                if (FirebaseApp.getApps(this).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyA5HC8LknVcWJssKb6zYxAJOqBo-l-nnHI")
                        .setApplicationId("1:786049368198:web:481e45fb1994a4d8db5107")
                        .setProjectId("kilagbea-d53e6")
                        .setStorageBucket("kilagbea-d53e6.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
                db = FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val riderPhone = intent?.getStringExtra("riderPhone")
        if (!riderPhone.isNullOrEmpty() && riderPhone != currentRiderPhone) {
            currentRiderPhone = riderPhone
            notifiedOrderIds.clear()
            startListeningToOrders(riderPhone)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ki-Lagbe Rider Is Online 🚴")
            .setContentText("You are on-duty and will receive new orders.")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun startListeningToOrders(riderPhone: String) {
        orderListener?.remove()
        
        // Ensure Firebase Firestore is initialized
        initFirebase()
        val firestore = db ?: return

        orderListener = firestore.collection("orders")
            .whereEqualTo("riderPhone", riderPhone)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                snapshot.documentChanges.forEach { change ->
                    val data = change.document.data
                    val orderID = change.document.id
                    val order = data.toOrder(orderID)

                    if ((change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) &&
                        order.status == "Assigned"
                    ) {
                        // Check if we already notified for this order update to prevent repeating sounds on the same order
                        if (!notifiedOrderIds.contains(order.id)) {
                            notifiedOrderIds.add(order.id)
                            NotificationHelper.showOrderNotification(
                                context = applicationContext,
                                orderIdStr = order.oID.toString(),
                                area = order.area.ifEmpty { "your area" }
                            )
                        }
                    } else if (order.status != "Assigned") {
                        // If order is no longer in Assigned state (e.g. accepted, cancelled, delivered), clean up from notified list
                        notifiedOrderIds.remove(order.id)
                    }
                }
            }
    }

    override fun onDestroy() {
        orderListener?.remove()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
