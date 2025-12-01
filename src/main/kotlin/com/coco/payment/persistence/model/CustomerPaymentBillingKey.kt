package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.PaymentSystem

class CustomerPaymentBillingKey(
    val customerSeq: Long,
    val paymentSystem: PaymentSystem,
    val billingKey: String
) {
}