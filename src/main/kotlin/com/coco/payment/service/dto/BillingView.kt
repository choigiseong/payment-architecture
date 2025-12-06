package com.coco.payment.service.dto

import com.coco.payment.persistence.enumerator.PaymentSystem

interface BillingView {

    data class BillingKeyDto(
        val paymentSystem: PaymentSystem,
        val billingKey: String,
        val cardNumber: String,
        val cardCompany: String
    )

    data class ConfirmBillingDto(
        val paymentSystem: PaymentSystem,
        val billingKey: String,
        val amount: Long,
        val customerEmail: String,
        val customerName: String,
        val orderId: String,
        val orderName: String
    )
}