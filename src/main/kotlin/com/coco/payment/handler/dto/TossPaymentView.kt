package com.coco.payment.handler.dto

import java.time.Instant

interface TossPaymentView {

    data class TossBillingKeyRequest(
        val customerKey: String,
        val authKey: String
    )

    data class TossBillingKeyResponse(
        val mId: String,
        val customerKey: String,
        val authenticatedAt: Instant,
        val method: String,
        val billingKey: String,
        val card: Card,
        val cardCompany: String,
        val cardNumber: String
    ) {
        data class Card(
            val issuerCode: String,
            val acquirerCode: String,
            val number: String,
            val cardType: String,
            val ownerType: String
        )
    }

    data class TossConfirmBillingRequest(
        val customerKey: String,
        val amount: Long,
        val customerEmail: String,
        val customerName: String,
        val orderId: String,
        val orderName: String,
    )

    data class TossConfirmBillingResponse(
        val mId: String,
        val lastTransactionKey: String,
        val paymentKey: String,
        val orderId: String
    )

}