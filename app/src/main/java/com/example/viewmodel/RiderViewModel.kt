package com.example.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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
    Home, Wallet, Profile
}

class RiderViewModel : ViewModel() {

    private var db: FirebaseFirestore? = null
    private var riderListener: ListenerRegistration? = null
    private var orderListener: ListenerRegistration? = null
    private var locationListener: ListenerRegistration? = null

    private var mediaPlayer: MediaPlayer? = null

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
                    
                    isOnline.value = rider.dutyStatus == "online"
                    
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
                        hasAssignedNow = order
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
                    NotificationHelper.showOrderNotification(
                        context = context,
                        orderIdStr = hasAssignedNow.oID.toString(),
                        area = hasAssignedNow.area.ifEmpty { "your area" }
                    )
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

    fun setOnlineStatus(online: Boolean) {
        val rider = currentRider.value ?: return
        val firestore = db ?: return
        
        isOnline.value = online
        firestore.collection("riders").document(rider.phone)
            .update("dutyStatus", if (online) "online" else "offline")
            
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
        val calendar = java.util.Calendar.getInstance()
        
        // Find Today's Epoch Boundaries
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        val yesterdayStart = todayStart - 86400000L
        val weekStart = todayStart - (7 * 86400000L)

        var dashCash = 0.0
        var dashEarnings = 0.0
        var activeDue = 0.0

        val ledgerOrders = allOrders.filter { it.status == "Delivered" || it.status == "Cancelled" }
        // Sort ascending to evaluate chronological balances
        val sortedLedger = ledgerOrders.sortedBy { it.time }

        val filteredOrders = mutableListOf<Order>()
        var completedCount = 0
        var cancelledCount = 0

        sortedLedger.forEach { o ->
            if (o.status == "Delivered") {
                val fee = o.riderFee ?: o.deliveryFee
                val earn = fee + o.surcharge
                
                var buyCost = 0.0
                o.items.forEach { item ->
                    buyCost += item.buy * item.qty
                }
                
                val dueChange = o.total - (earn + buyCost)
                
                // Track dynamic running balances
                if (!o.cashSettled) {
                    activeDue += dueChange
                }
            }
        }

        // Apply filters to compile statistics
        allOrders.forEach { o ->
            val orderTime = o.time
            if (orderTime >= todayStart && o.status == "Delivered") {
                dashCash += o.total
                val fee = o.riderFee ?: o.deliveryFee
                dashEarnings += (fee + o.surcharge)
            }

            var inFilter = false
            when (filter) {
                "today" -> if (orderTime >= todayStart) inFilter = true
                "yesterday" -> if (orderTime >= yesterdayStart && orderTime < todayStart) inFilter = true
                "week" -> if (orderTime >= weekStart) inFilter = true
            }

            if (inFilter && (o.status == "Delivered" || o.status == "Cancelled")) {
                filteredOrders.add(o)
                if (o.status == "Delivered") completedCount++ else if (o.status == "Cancelled") cancelledCount++
            }
        }

        // Assign derived values safely to states
        walletCollected.value = dashCash
        walletEarnings.value = dashEarnings
        walletDueAmount.value = activeDue
        walletDeliveredCount.value = completedCount
        walletCancelledCount.value = cancelledCount

        // Render Ledger item values
        var runningCumBalance = 0.0
        val historyDetails = ledgerOrders.sortedBy { it.time }.map { o ->
            val fee = o.riderFee ?: o.deliveryFee
            val earn = fee + o.surcharge
            var buyCost = 0.0
            o.items.forEach { item -> buyCost += item.buy * item.qty }
            val cDiff = o.total - (earn + buyCost)
            
            if (!o.cashSettled) {
                runningCumBalance += cDiff
            }
            
            // Return modified order replica carrying UI markers
            o.copy().apply {
                id = o.id
                // Store temporary marker details inside standard attributes or tagging if needed
            }
        }
        
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
        if (!isOnline.value) return
        mediaPlayer?.release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource("https://actions.google.com/sounds/v1/alarms/alarm_clock.ogg")
                isLooping = true
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    return OrderItem(
        name = this["name"] as? String ?: "",
        qty = (this["qty"] as? Number)?.toInt() ?: 1,
        buy = (this["buy"] as? Number)?.toInt() ?: 0,
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

    return Order(
        id = id,
        oID = (this["oID"] as? Number)?.toLong() ?: 0L,
        name = this["name"] as? String ?: "",
        phone = this["phone"] as? String ?: "",
        area = this["area"] as? String ?: "",
        address = this["address"] as? String ?: "",
        status = this["status"] as? String ?: "Pending",
        riderPhone = this["riderPhone"] as? String ?: "",
        items = itemsList,
        total = (this["total"] as? Number)?.toDouble() ?: 0.0,
        subtotal = (this["subtotal"] as? Number)?.toDouble() ?: 0.0,
        deliveryFee = (this["deliveryFee"] as? Number)?.toDouble() ?: 40.0,
        riderFee = (this["riderFee"] as? Number)?.toDouble(),
        handlingFee = (this["handlingFee"] as? Number)?.toDouble() ?: 0.0,
        surcharge = (this["surcharge"] as? Number)?.toDouble() ?: 0.0,
        note = this["note"] as? String ?: "",
        riderAssignedAt = (this["riderAssignedAt"] as? Number)?.toLong() ?: 0L,
        time = (this["time"] as? Number)?.toLong() ?: 0L,
        deliveredAt = (this["deliveredAt"] as? Number)?.toLong() ?: 0L,
        cashSettled = this["cashSettled"] as? Boolean ?: false,
        stockDeducted = this["stockDeducted"] as? Boolean ?: false
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
