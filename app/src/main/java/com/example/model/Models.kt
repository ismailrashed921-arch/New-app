package com.example.model

data class Rider(
    val name: String = "",
    val phone: String = "",
    val pin: String = "",
    val area: String = "",
    val photo: String = "",
    val nid: String = "",
    val nidBack: String = "",
    val status: String = "pending", // active, pending, etc.
    val dutyStatus: String = "offline", // online, offline
    val time: Long = 0L
)

data class OrderItem(
    val name: String = "",
    val qty: Int = 1,
    val buy: Int = 0,
    val source: String = "Ki-Lagbe Shop",
    val variant: String = "",
    val img: String = ""
)

data class Order(
    var id: String = "",
    val oID: Long = 0L,
    val name: String = "",
    val phone: String = "",
    val area: String = "",
    val address: String = "",
    val status: String = "Pending", // Assigned, Accepted, Processing, On the Way, Delivered, Cancelled
    val riderPhone: String = "",
    val items: List<OrderItem> = emptyList(),
    val total: Double = 0.0,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 40.0,
    val riderFee: Double? = null,
    val handlingFee: Double = 0.0,
    val surcharge: Double = 0.0,
    val note: String = "",
    val riderAssignedAt: Long = 0L,
    val time: Long = 0L,
    val deliveredAt: Long = 0L,
    val cashSettled: Boolean = false,
    val stockDeducted: Boolean = false
)
