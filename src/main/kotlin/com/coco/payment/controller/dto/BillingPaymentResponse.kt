package com.coco.payment.controller.dto

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import com.coco.payment.service.dto.BillingPaymentResult

data class BillingPaymentResponse(
    val orderKey: String,
    val paymentKey: String,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentTransactionStatus,
    val tid: String?,
    val code: String?,
    val message: String?,
)

data class PaymentPollingResponse(
    val orderKey: String,
    val paymentKey: String,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentTransactionStatus,
    val tid: String?,
)

fun BillingPaymentResult.toResponse() = BillingPaymentResponse(orderKey, paymentKey, orderStatus, paymentStatus, tid, code, message)
fun BillingPaymentResult.toPollingResponse() = PaymentPollingResponse(orderKey, paymentKey, orderStatus, paymentStatus, tid)
