package com.coco.payment.service.dto

import com.coco.payment.persistence.enumerator.PaymentSystem
import java.time.Instant

interface BillingView {

    data class BillingKeyResult(
        val paymentSystem: PaymentSystem,
        val billingKey: String,
        val cardNumber: String,
        val cardCompany: String
    )

    data class ConfirmBillingCommand(
        val customerKey: String,
        val paymentSystem: PaymentSystem,
        val amount: Long,
        val customerEmail: String,
        val customerName: String,
        val orderId: String,
        val orderName: String
    )

    data class ConfirmBillingResult(
        val orderId: String,
        val paymentKey: String,
        val paymentSystem: PaymentSystem,
        val status: String,
        val requestedAt: Instant,
        val approvedAt: Instant,
        val taxFreeAmount: Long
    )

}