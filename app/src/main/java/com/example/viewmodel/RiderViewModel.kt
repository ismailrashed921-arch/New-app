package com.example.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Order
import com.example.model.OrderItem
import com.example.model.Rider
import com.example.network.CloudinaryUploader
import com.example.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LoginState {
    object Idle : LoginState
    object Loading : LoginState
    object Success : LoginState
    data class Error(val message: String) : LoginState
}

sealed interface SignUpState {
    object Idle : SignUpState
    data class Progress(val message: String) : SignUpState
    object Success : SignUpState
    data class Error(val message: String) : SignUpState
}

enum class ActiveTab {
    Home, Orders, Wallet, Profile
}

class RiderViewModel : ViewModel() {

    private var db: FirebaseFirestore? = null
    private var riderListener: ListenerRegistration? = null
    private var orderListener: ListenerRegistration? = null
    private var locationListener: ListenerRegistration? = null

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: android.os.Vibrator? = null
    private val notifiedOrderIds = HashSet<String>()

    // Exposed States
    val loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    
    val locations = MutableStateFlow<List<String>>(listOf("Dhanmondi", "Mohammadpur"))
    val currentRider = MutableStateFlow<Rider?>(null)
    
    val activeOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrdersList = MutableStateFlow<List<Order>>(emptyList())
    val currentActiveOrderId = MutableStateFlow<String?>(null)
    
    val activeTab = MutableStateFlow(ActiveTab.Home)
    val isOnline = MutableStateFlow(false)
    val assignedOrderPopup = MutableStateFlow<Order?>(null)
    
    // Financial State (Wallet)
    val selectedFilter = MutableStateFlow("today") // today, yesterday, week
    val walletEarnings = MutableStateFlow(0.0)
    val walletCollected = MutableStateFlow(0.0)
    val walletDeliveredCount = MutableStateFlow(0)
    val walletCancelledCount = MutableStateFlow(0)
    val walletDueAmount = MutableStateFlow(0.0) // Red if positive, Green if negative
    val transactionHistory = MutableStateFlow<List<Order>>(emptyList())

    // Pin State
    val pinChangeState = MutableStateFlow<String?>(null) // result message

    private fun initFirebase(context: Context) {
        if (db == null) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyA5HC8LknVcWJssKb6zYxAJOqBo-l-nnHI")
                        .setApplicationId("1:786049368198:web:481e45fb1994a4d8db5107")
                        .setProjectId("kilagbea-d53e6")
                        .setStorageBucket("kilagbea-d53e6.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
                db = FirebaseFirestore.getInstance()
                listenToLocations()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startApp(context: Context, phone: String) {
        initFirebase(context)
        loadRider(context, phone)
    }

    private fun listenToLocations() {
        val firestore = db ?: return
        locationListener?.remove()
        locationListener = firestore.collection("locations")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = mutableListOf("Dhanmondi", "Mohammadpur")
                snapshot.forEach { doc ->
                    val name = doc.getString("name") ?: ""
                    if (name.isNotEmpty() && name != "Dhanmondi" && name != "Mohammadpur") {
                        list.add(name)
                    }
                }
                locations.value = list
            }
    }

    fun login(context: Context, phone: String, pin: String) {
        initFirebase(context)
        val firestore = db ?: run {
            loginState.value = LoginState.Error("Firebase not ready")
            return
        }

        loginState.value = LoginState.Loading
        firestore.collection("riders").document(phone).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val riderPin = doc.getString("pin") ?: ""
                    if (riderPin == pin) {
                        val riderStatus = doc.getString("status") ?: "pending"
                        if (riderStatus != "active") {
                            loginState.value = LoginState.Error("Your account is not Active. Please contact Admin.")
                            return@addOnSuccessListener
                        }
                        
                        // Save to preferences
                        val prefs = context.getSharedPreferences("RiderPrefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("riderPhone", phone).apply()
                        
                        loginState.value = LoginState.Success
                        loadRider(context, phone)
                    } else {
                        loginState.value = LoginState.Error("Invalid Phone or PIN!")
                    }
                } else {
                    loginState.value = LoginState.Error("Invalid Phone or PIN!")
                }
            }
            .addOnFailureListener {
                loginState.value = LoginState.Error("Network Error. Check connection.")
            }
    }

    fun signUp(
        context: Context,
        name: String,
        phone: String,
        pin: String,
        area: String,
        photoUri: Uri?,
        nidFrontUri: Uri?,
        nidBackUri: Uri?
    ) {
        initFirebase(context)
        val firestore = db ?: run {
            signUpState.value = SignUpState.Error("Firebase is not initialized")
            return
        }

        if (name.isEmpty() || phone.isEmpty() || pin.length != 6 || area.isEmpty() || photoUri == null || nidFrontUri == null || nidBackUri == null) {
            signUpState.value = SignUpState.Error("Please fill all fields and upload all required images")
            return
        }

        signUpState.value = SignUpState.Progress("Uploading documents to Cloudinary...")

        viewModelScope.launch {
            try {
                // Upload files consecutively to secure handles
                val photoUrl = CloudinaryUploader.uploadImageUri(context, photoUri)
                if (photoUrl == null) {
                    signUpState.value = SignUpState.Error("Failed to upload Profile Selfie")
                    return@launch
                }

                val nidFrontUrl = CloudinaryUploader.uploadImageUri(context, nidFrontUri)
                if (nidFrontUrl == null) {
                    signUpState.value = SignUpState.Error("Failed to upload NID Front")
                    return@launch
                }

                val nidBackUrl = CloudinaryUploader.uploadImageUri(context, nidBackUri)
                if (nidBackUrl == null) {
                    signUpState.value = SignUpState.Error("Failed to upload NID Back")
                    return@launch
                }

                signUpState.value = SignUpState.Progress("Saving Application Details...")

                val riderData = mapOf(
                    "name" to name,
                    "phone" to phone,
                    "pin" to pin,
                    "area" to area,
                    "photo" to photoUrl,
                    "nid" to nidFrontUrl,
                    "nidBack" to nidBackUrl,
                    "status" to "pending",
                    "dutyStatus" to "offline",
                    "time" to System.currentTimeMillis()
                )

                firestore.collection("riders").document(phone).set(riderData)
                    .addOnSuccessListener {
                        signUpState.value = SignUpState.Success
                    }
                    .addOnFailureListener { e ->
                        signUpState.value = SignUpState.Error("Error saving application: ${e.message}")
                    }

            } catch (e: Exception) {
                signUpState.value = SignUpState.Error("Upload Error: ${e.message}")
            }
        }
    }

    private fun loadRider(context: Context, phone: String) {
        val firestore = db ?: return
        riderListener?.remove()
        riderListener = firestore.collection("riders").document(phone)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                if (snapshot.exists()) {
                    val map = snapshot.data
                    val rider = map.toRider()
                    currentRider.value = rider
                    
                    if (rider.status != "active") {
                        logout(context)
                        return@addSnapshotListener
                    }
                    
                    val prefs = context.getSharedPreferences("RiderPrefs", Context.MODE_PRIVATE)
                    val localOnlineOverride = if (prefs.contains("dutyOnline")) {
                        prefs.getBoolean("dutyOnline", false)
                    } else {
                        val serverOnline = rider.dutyStatus == "online"
                        prefs.edit().putBoolean("dutyOnline", serverOnline).apply()
                        serverOnline
                    }
                    isOnline.value = localOnlineOverride
                    
                    // Listen to orders
                    listenToOrders(context, phone)
                }
            }
    }

    private fun listenToOrders(context: Context, riderPhone: String) {
        val firestore = db ?: return
        orderListener?.remove()
        orderListener = firestore.collection("orders")
            .whereEqualTo("riderPhone", riderPhone)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                
                val allOrdersTemp = mutableListOf<Order>()
                val activeOrdersTemp = mutableListOf<Order>()
                var hasAssignedNow: Order? = null

                snapshot.documentChanges.forEach { change ->
                    val data = change.document.data
                    val orderID = change.document.id
                    val order = data.toOrder(orderID)
                    
                    if ((change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED || 
                         change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) && 
                        order.status == "Assigned" && isOnline.value) {
                        if (!notifiedOrderIds.contains(order.id)) {
                            notifiedOrderIds.add(order.id)
                            hasAssignedNow = order
                        }
                    } else if (order.status != "Assigned") {
                        notifiedOrderIds.remove(order.id)
                    }
                }

                snapshot.forEach { doc ->
                    val order = doc.data.toOrder(doc.id)
                    allOrdersTemp.add(order)
                    if (order.status != "Delivered" && order.status != "Cancelled") {
                        activeOrdersTemp.add(order)
                    }
                }

                // If popup has an assigned order which was subsequently modified or accepted
                val currentlyPopup = assignedOrderPopup.value
                if (currentlyPopup != null) {
                    val actualMatch = allOrdersTemp.find { it.id == currentlyPopup.id }
                    if (actualMatch == null || actualMatch.status != "Assigned") {
                        assignedOrderPopup.value = null
                        stopAlarm()
                    }
                }

                if (hasAssignedNow != null) {
                    assignedOrderPopup.value = hasAssignedNow
                    playAlarm(context)
                }

                // Manage active orders
                allOrdersList.value = allOrdersTemp
                activeOrdersTemp.sortBy { it.time }
                activeOrders.value = activeOrdersTemp

                val currentActiveId = currentActiveOrderId.value
                val stillExists = activeOrdersTemp.find { it.id == currentActiveId }
                if (stillExists == null && activeOrdersTemp.isNotEmpty()) {
                    currentActiveOrderId.value = activeOrdersTemp[0].id
                } else if (activeOrdersTemp.isEmpty()) {
                    currentActiveOrderId.value = null
                }

                // Re-evaluate wallet performance metrics
                evaluateWalletRecords()
            }
    }

    fun setOnlineStatus(online: Boolean, context: Context? = null) {
        if (db == null && context != null) {
            initFirebase(context)
        }
        val firestore = db ?: return
        val phone = currentRider.value?.phone 
            ?: context?.getSharedPreferences("RiderPrefs", Context.MODE_PRIVATE)?.getString("riderPhone", null)
            
        if (phone.isNullOrEmpty()) {
            if (context != null) {
                Toast.makeText(context, "Error: Rider phone not found!", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        isOnline.value = online
        if (context != null) {
            context.getSharedPreferences("RiderPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("dutyOnline", online)
                .apply()
        }
        firestore.collection("riders").document(phone)
            .update("dutyStatus", if (online) "online" else "offline")
            .addOnSuccessListener {
                if (context != null) {
                    Toast.makeText(
                        context,
                        if (online) "You are now ONLINE 🚴" else "You are now OFFLINE 💤",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                // Do NOT revert isOnline.value on failure. Keep the user's toggle state!
                if (context != null) {
                    val msg = e.message ?: ""
                    if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                        Toast.makeText(
                            context,
                            if (online) "You are now ONLINE (Local Mode) 🚴" else "You are now OFFLINE (Local Mode) 💤",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            if (online) "You are now ONLINE 🚴" else "You are now OFFLINE 💤",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            
        if (!online) {
            assignedOrderPopup.value = null
            stopAlarm()
        }
    }

    fun acceptOrder(orderId: String) {
        val firestore = db ?: return
        
        firestore.collection("orders").document(orderId)
            .update(
                mapOf(
                    "status" to "Accepted",
                    "riderAssignedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                assignedOrderPopup.value = null
                stopAlarm()
                currentActiveOrderId.value = orderId
            }
    }

    fun updateStatus(orderId: String, newStatus: String, callback: (Boolean) -> Unit) {
        val firestore = db ?: return
        
        if (newStatus == "Delivered") {
            // Transaction-aware delivery logic
            firestore.collection("orders").document(orderId).get()
                .addOnSuccessListener { doc ->
                    val order = doc.data.toOrder(doc.id)
                    val updateMap = mutableMapOf<String, Any>(
                        "status" to "Delivered",
                        "deliveredAt" to System.currentTimeMillis()
                    )
                    
                    if (!order.stockDeducted) {
                        updateMap["stockDeducted"] = true
                        // Deduct product quantities
                        order.items.forEach { item ->
                            firestore.collection("products")
                                .whereEqualTo("name", item.name).get()
                                .addOnSuccessListener { pSnap ->
                                    if (!pSnap.isEmpty) {
                                        val pDoc = pSnap.documents[0]
                                        val currentStock = pDoc.getLong("stock") ?: 0
                                        var newStock = currentStock - item.qty
                                        if (newStock < 0) newStock = 0
                                        firestore.collection("products").document(pDoc.id)
                                            .update("stock", newStock)
                                    }
                                }
                        }
                    }

                    firestore.collection("orders").document(orderId).update(updateMap)
                        .addOnSuccessListener { callback(true) }
                        .addOnFailureListener { callback(false) }
                }
                .addOnFailureListener { callback(false) }
        } else {
            firestore.collection("orders").document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { callback(false) }
        }
    }

    fun setFilter(filter: String) {
        selectedFilter.value = filter
        evaluateWalletRecords()
    }

    private fun evaluateWalletRecords() {
        val filter = selectedFilter.value
        val allOrders = allOrdersList.value

        val now = System.currentTimeMillis()
        
        // Find Today's Epoch Boundaries safely representing both Bangladesh GMT+6 and Local device timezone midnights
        val targetZone = java.util.TimeZone.getTimeZone("GMT+6")
        val calendarTarget = java.util.Calendar.getInstance(targetZone)
        calendarTarget.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendarTarget.set(java.util.Calendar.MINUTE, 0)
        calendarTarget.set(java.util.Calendar.SECOND, 0)
        calendarTarget.set(java.util.Calendar.MILLISECOND, 0)
        val todayStartTarget = calendarTarget.timeInMillis

        val calendarLocal = java.util.Calendar.getInstance()
        calendarLocal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendarLocal.set(java.util.Calendar.MINUTE, 0)
        calendarLocal.set(java.util.Calendar.SECOND, 0)
        calendarLocal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStartLocal = calendarLocal.timeInMillis

        // Get the wider/most encompassing start of today
        val todayStart = minOf(todayStartTarget, todayStartLocal)
        val yesterdayStart = todayStart - 86400000L
        val weekStart = todayStart - (7 * 86400000L)

        var dashCash = 0.0
        var dashEarnings = 0.0
        var activeDue = 0.0

        val ledgerOrders = allOrders.filter { it.status == "Delivered" || it.status == "Cancelled" }
        // Sort ascending to evaluate chronological balances
        val sortedLedger = ledgerOrders.sortedBy { it.time }

        // Step 1: Calculate chronological Ledger changes and running balances
        sortedLedger.forEach { o ->
            if (o.status == "Delivered") {
                val fee = o.riderFee ?: o.deliveryFee
                val earn = fee + o.surcharge
                
                var buyCost = 0.0
                o.items.forEach { item ->
                    buyCost += item.buy * item.qty
                }
                
                val dueChange = o.total - (earn + buyCost)
                o.dueChangeCustom = dueChange
                
                if (!o.cashSettled) {
                    activeDue += dueChange
                    o.runningBal = activeDue
                    o.isSettledCustom = false
                } else {
                    o.runningBal = 0.0
                    o.isSettledCustom = true
                }
            } else {
                o.dueChangeCustom = 0.0
                o.runningBal = activeDue
                o.isSettledCustom = o.cashSettled
            }
        }

        val filteredOrders = mutableListOf<Order>()
        var completedCount = 0
        var cancelledCount = 0

        // Step 2: Extract filtered orders & compute stats
        sortedLedger.forEach { o ->
            // Use deliveredAt for completed orders so stats count on actual completion day
            var orderTime = if (o.status == "Delivered" && o.deliveredAt > 0L) {
                o.deliveredAt
            } else if (o.status == "Cancelled" && o.cancelledAt > 0L) {
                o.cancelledAt
            } else if (o.status == "Cancelled" && o.updatedAt > 0L) {
                o.updatedAt
            } else if (o.riderAssignedAt > 0L) {
                o.riderAssignedAt
            } else {
                o.time
            }
            if (orderTime <= 0L) {
                orderTime = System.currentTimeMillis()
            }

            // Calculate Tab Filter Matches
            var match = false
            when (filter) {
                "today" -> if (orderTime >= todayStart) match = true
                "yesterday" -> if (orderTime >= yesterdayStart && orderTime < todayStart) match = true
                "week" -> if (orderTime >= weekStart) match = true
            }

            if (match) {
                filteredOrders.add(o)
                if (o.status == "Delivered") {
                    completedCount++
                    val fee = o.riderFee ?: o.deliveryFee
                    dashCash += o.total
                    dashEarnings += (fee + o.surcharge)
                } else if (o.status == "Cancelled") {
                    cancelledCount++
                }
            }
        }

        // Assign derived values safely to states
        walletCollected.value = dashCash
        walletEarnings.value = dashEarnings
        walletDueAmount.value = activeDue
        walletDeliveredCount.value = completedCount
        walletCancelledCount.value = cancelledCount

        // Sort descending by time for UI ledger/history list
        filteredOrders.sortByDescending { it.time }
        transactionHistory.value = filteredOrders
    }

    fun changePin(oldPin: String, newPin: String) {
        val rider = currentRider.value ?: return
        val firestore = db ?: return

        if (oldPin != rider.pin) {
            pinChangeState.value = "Current PIN is incorrect!"
            return
        }
        if (newPin.length != 6) {
            pinChangeState.value = "New PIN must be exactly 6 digits."
            return
        }

        firestore.collection("riders").document(rider.phone)
            .update("pin", newPin)
            .addOnSuccessListener {
                pinChangeState.value = "PIN Updated Successfully!"
            }
            .addOnFailureListener {
                pinChangeState.value = "Error updating PIN!"
            }
    }

    fun clearPinChangeState() {
        pinChangeState.value = null
    }

    private fun playAlarm(context: Context) {
        // Handled centrally by RiderService in the background and foreground
    }

    private fun stopAlarm() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logout(context: Context) {
        setOnlineStatus(false)
        
        riderListener?.remove()
        orderListener?.remove()
        locationListener?.remove()
        
        riderListener = null
        orderListener = null
        locationListener = null
        
        currentRider.value = null
        activeOrders.value = emptyList()
        allOrdersList.value = emptyList()
        
        val prefs = context.getSharedPreferences("RiderPrefs", Context.MODE_PRIVATE)
        prefs.edit().remove("riderPhone").apply()
        
        loginState.value = LoginState.Idle
        signUpState.value = SignUpState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stopAlarm()
        riderListener?.remove()
        orderListener?.remove()
        locationListener?.remove()
    }
}

// Map parsers
fun Map<String, Any>?.toOrderItem(): OrderItem {
    if (this == null) return OrderItem()
    
    val qty = when (val raw = this["qty"]) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull() ?: 1
        else -> 1
    }
    val buy = when (val raw = this["buy"]) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull() ?: 0
        else -> 0
    }

    return OrderItem(
        name = this["name"] as? String ?: "",
        qty = qty,
        buy = buy,
        source = this["source"] as? String ?: "Ki-Lagbe Shop",
        variant = this["variant"] as? String ?: "",
        img = this["img"] as? String ?: ""
    )
}

fun Map<String, Any>?.toOrder(id: String): Order {
    if (this == null) return Order()
    
    val itemsRaw = this["items"]
    val itemsList = mutableListOf<OrderItem>()
    if (itemsRaw is List<*>) {
        itemsRaw.forEach { itemObj ->
            if (itemObj is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                itemsList.add((itemObj as Map<String, Any>).toOrderItem())
            }
        }
    } else if (itemsRaw is Map<*, *>) {
        itemsRaw.forEach { (_, itemObj) ->
            if (itemObj is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                itemsList.add((itemObj as Map<String, Any>).toOrderItem())
            }
        }
    }

    val rawOID = this["oID"] ?: this["orderId"] ?: this["orderID"]
    val oID = when (rawOID) {
        null -> id
        is Number -> {
            val numL = rawOID.toLong()
            if (numL > 0L) numL.toString() else id
        }
        is String -> {
            val s = rawOID.toString().trim()
            if (s.isEmpty()) id else s
        }
        else -> {
            val s = rawOID.toString().trim()
            if (s.isEmpty()) id else s
        }
    }

    val total = when (val raw = this["total"]) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    val subtotal = when (val raw = this["subtotal"]) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    val deliveryFee = when (val raw = this["deliveryFee"]) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull() ?: 40.0
        else -> 40.0
    }
    val riderFee = when (val raw = this["riderFee"]) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull()
        else -> null
    }
    val handlingFee = when (val raw = this["handlingFee"]) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    val surcharge = when (val raw = this["surcharge"]) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    val parseTime: (Any?) -> Long = { raw ->
        if (raw == null) {
            0L
        } else {
            when (raw) {
                is Number -> raw.toLong()
                is String -> {
                    val s = raw.trim()
                    val longVal = s.toLongOrNull()
                    if (longVal != null) {
                        longVal
                    } else {
                        val formats = listOf(
                            "yyyy-MM-dd HH:mm:ss",
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            "yyyy-MM-dd'T'HH:mm:ss",
                            "yyyy-MM-dd",
                            "dd MMM yyyy, hh:mm a",
                            "dd/MM/yyyy HH:mm:ss",
                            "dd/MM/yyyy"
                        )
                        var parsedTime = 0L
                        for (fmt in formats) {
                            try {
                                val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
                                if (fmt.contains("'Z'")) {
                                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }
                                val d = sdf.parse(s)
                                if (d != null) {
                                    parsedTime = d.time
                                    break
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                        parsedTime
                    }
                }
                is com.google.firebase.Timestamp -> raw.toDate().time
                else -> {
                    if (raw::class.java.simpleName == "Timestamp") {
                        try {
                            val method = raw::class.java.getMethod("toDate")
                            val date = method.invoke(raw) as java.util.Date
                            date.time
                        } catch (ex: Exception) {
                            0L
                        }
                    } else {
                        0L
                    }
                }
            }
        }
    }

    val riderAssignedAt = parseTime(this["riderAssignedAt"])
    val time = parseTime(this["time"])
    val deliveredAt = parseTime(this["deliveredAt"])
    val cancelledAt = parseTime(this["cancelledAt"] ?: this["canceledAt"] ?: this["cancelled_at"] ?: this["canceled_at"] ?: this["cancelledTime"] ?: this["canceledTime"])
    val updatedAt = parseTime(this["updatedAt"] ?: this["updated_at"] ?: this["updatedTime"] ?: this["lastUpdated"])

    val rawStatus = (this["status"] as? String)?.trim() ?: "Pending"
    val status = if (rawStatus.equals("delivered", ignoreCase = true)) "Delivered"
                else if (rawStatus.equals("cancelled", ignoreCase = true) || rawStatus.equals("canceled", ignoreCase = true) || rawStatus.contains("cancel", ignoreCase = true) || rawStatus.equals("rejected", ignoreCase = true) || rawStatus.equals("reject", ignoreCase = true)) "Cancelled"
                else if (rawStatus.equals("assigned", ignoreCase = true)) "Assigned"
                else if (rawStatus.equals("accepted", ignoreCase = true)) "Accepted"
                else if (rawStatus.equals("pending", ignoreCase = true)) "Pending"
                else if (rawStatus.equals("processing", ignoreCase = true)) "Processing"
                else if (rawStatus.equals("on the way", ignoreCase = true) || rawStatus.equals("on_the_way", ignoreCase = true) || rawStatus.equals("ontheway", ignoreCase = true)) "On the Way"
                else rawStatus

    return Order(
        id = id,
        oID = oID,
        name = this["name"] as? String ?: "",
        phone = this["phone"] as? String ?: "",
        area = this["area"] as? String ?: "",
        address = this["address"] as? String ?: "",
        status = status,
        riderPhone = this["riderPhone"] as? String ?: "",
        items = itemsList,
        total = if (total > 0.0) total else (subtotal + deliveryFee + surcharge + handlingFee),
        subtotal = subtotal,
        deliveryFee = deliveryFee,
        riderFee = riderFee,
        handlingFee = handlingFee,
        surcharge = surcharge,
        note = this["note"] as? String ?: "",
        riderAssignedAt = riderAssignedAt,
        time = time,
        deliveredAt = deliveredAt,
        cancelledAt = cancelledAt,
        updatedAt = updatedAt,
        cashSettled = when (val raw = this["cashSettled"]) {
            is Boolean -> raw
            is String -> raw.toBoolean()
            is Number -> raw.toInt() == 1
            else -> false
        },
        stockDeducted = when (val raw = this["stockDeducted"]) {
            is Boolean -> raw
            is String -> raw.toBoolean()
            is Number -> raw.toInt() == 1
            else -> false
        }
    )
}

fun Map<String, Any>?.toRider(): Rider {
    if (this == null) return Rider()
    return Rider(
        name = this["name"] as? String ?: "",
        phone = this["phone"] as? String ?: "",
        pin = this["pin"] as? String ?: "",
        area = this["area"] as? String ?: "",
        photo = this["photo"] as? String ?: "",
        nid = this["nid"] as? String ?: "",
        nidBack = this["nidBack"] as? String ?: "",
        status = this["status"] as? String ?: "pending",
        dutyStatus = this["dutyStatus"] as? String ?: "offline",
        time = (this["time"] as? Number)?.toLong() ?: 0L
    )
}
