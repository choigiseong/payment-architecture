package com.coco.payment.service.dto

data class BillingPaymentCommand(
    val companySeq: Long,
    val orderKey: String,
    val paymentKey: String,
    val orderName: String,
    val totalPrice: Long,
    val items: List<BillingOrderItem>,
)

{
    companion object {
        fun of(
            companySeq: Long,
            orderKey: String,
            paymentKey: String,
            orderName: String,
            totalPrice: Long,
            items: List<BillingOrderItem>,
        ) = BillingPaymentCommand(companySeq, orderKey, paymentKey, orderName, totalPrice, items)
    }
}

data class BillingOrderItem(val itemName: String, val unitPrice: Long, val quantity: Int)
