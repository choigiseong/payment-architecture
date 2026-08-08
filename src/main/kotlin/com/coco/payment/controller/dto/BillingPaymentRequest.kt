package com.coco.payment.controller.dto

import com.coco.payment.service.dto.BillingOrderItem
import com.coco.payment.service.dto.BillingPaymentCommand

data class BillingPaymentRequest(
    val companySeq: Long,
    val orderKey: String,
    val paymentKey: String,
    val orderName: String,
    val totalPrice: Long,
    val items: List<BillingPaymentItemRequest>,
) {
    fun toCommand(): BillingPaymentCommand {
        require(orderKey.isNotBlank()) { "orderKey must not be blank" }
        require(paymentKey.isNotBlank()) { "paymentKey must not be blank" }
        require(totalPrice > 0) { "totalPrice must be positive" }
        require(items.isNotEmpty()) { "items must not be empty" }
        return BillingPaymentCommand(companySeq, orderKey, paymentKey, orderName, totalPrice, items.map { BillingOrderItem(it.itemName, it.unitPrice, it.quantity) })
    }
}

data class BillingPaymentItemRequest(val itemName: String, val unitPrice: Long, val quantity: Int)
