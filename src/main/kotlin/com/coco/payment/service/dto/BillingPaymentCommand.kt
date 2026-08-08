package com.coco.payment.service.dto

data class BillingPaymentCommand(
    val companySeq: Long,
    val orderKey: String,
    val paymentKey: String,
    val orderName: String,
    val totalPrice: Long,
    val items: List<BillingOrderItem>,
)

data class BillingOrderItem(val itemName: String, val unitPrice: Long, val quantity: Int)
