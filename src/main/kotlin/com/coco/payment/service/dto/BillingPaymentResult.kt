package com.coco.payment.service.dto

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus

data class PrepareBillingPaymentResult(
    val orderId: Long,
    val paymentTransactionId: Long,
    val orderKey: String,
    val paymentKey: String,
    val billingKey: String,
    val customerKey: String,
    val moid: String,
    val orderName: String,
    val amount: Long,
)

data class BillingPaymentResult(
    val orderKey: String,
    val paymentKey: String,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentTransactionStatus,
    val tid: String?,
    val code: String?,
    val message: String?,
)
