package com.coco.payment.controller.dto

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import com.coco.payment.service.dto.BillingPaymentResult
import java.time.LocalDate

data class BillingPaymentResponse(
    val orderKey: String,
    val paymentKey: String,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentTransactionStatus,
    val tid: String?,
    val code: String?,
    val message: String?,
) {
    companion object {
        fun of(result: BillingPaymentResult) = BillingPaymentResponse(
            result.orderKey,
            result.paymentKey,
            result.orderStatus,
            result.paymentStatus,
            result.tid,
            result.code,
            result.message,
        )
    }
}

data class PaymentPollingResponse(
    val orderKey: String,
    val paymentKey: String,
    val orderStatus: OrderStatus,
    val paymentStatus: PaymentTransactionStatus,
    val deliveryDate: LocalDate,
    val tid: String?,
    val code: String?,
    val message: String?,
) {
    companion object {
        fun of(result: BillingPaymentResult) = PaymentPollingResponse(
            result.orderKey,
            result.paymentKey,
            result.orderStatus,
            result.paymentStatus,
            result.deliveryDate,
            result.tid,
            result.code,
            result.message,
        )
    }
}
