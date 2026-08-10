package com.coco.payment.service.dto

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus

sealed interface PrepareBillingPaymentResult {
    val orderId: Long
    val orderKey: String
    val paymentKey: String

    data class Ready(
        override val orderId: Long,
        val paymentTransactionId: Long,
        override val orderKey: String,
        override val paymentKey: String,
        val billingKey: String,
        val customerKey: String,
        val moid: String,
        val orderName: String,
        val amount: Long,
    ) : PrepareBillingPaymentResult

    data class AlreadyPending(
        override val orderId: Long,
        override val orderKey: String,
        override val paymentKey: String,
        val status: PaymentTransactionStatus,
        val tid: String?,
    ) : PrepareBillingPaymentResult
}

data class BillingPaymentResult(
    val orderKey: String,
    val paymentKey: String,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentTransactionStatus,
    val tid: String?,
    val code: String?,
    val message: String?,
)
