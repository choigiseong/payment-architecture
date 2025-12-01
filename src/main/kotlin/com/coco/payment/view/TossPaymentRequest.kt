package com.coco.payment.view

interface TossPaymentRequest {

    data class BillingKeyRequest(
        val customerKey: String,
        val authKey: String
    )
}