package com.coco.payment.service.dto

import com.coco.payment.persistence.enumerator.PaymentSystem

data class BillingKeyDto(
    val paymentSystem: PaymentSystem,
    val billingKey: String,
    val cardNumber: String,
    val cardCompany: String
) {
}