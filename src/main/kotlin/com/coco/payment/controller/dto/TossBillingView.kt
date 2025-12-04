package com.coco.payment.controller.dto

interface TossBillingView {

    data class BillingKeyRequest(
        val customerKey: String,
        val authKey: String
    ) {
    }

    data class ConfirmBillingRequest(
        val customerKey: String,
        val customerEmail: String,
        val amount: Long,
        val customerName: String,
        val orderId: String,
        val orderName: String,
    )

    data class ConfirmBillingResponse(
        val orderId: String
    )
}