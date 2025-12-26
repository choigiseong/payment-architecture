package com.coco.payment.handler.paymentgateway.dto

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

    data class TossConfirmBillingBillingResponse(
        val paymentKey: String,
        val type: String,
        val mId: String,
        val lastTransactionKey: String,
        val orderId: String,
        val totalAmount: Long,
        val balanceAmount: Long,
        val status: String,
        val requestedAt: Instant,
        val approvedAt: Instant,
        val taxFreeAmount: Long
    )

    data class TossTransactionResponse(
        val paymentKey: String,
        val type: String,
        val mId: String,
        val lastTransactionKey: String,
        val orderId: String,
        val totalAmount: Long,
        val balanceAmount: Long,
        val status: String,
        val requestedAt: Instant,
        val approvedAt: Instant,
        val taxFreeAmount: Long
    )



}