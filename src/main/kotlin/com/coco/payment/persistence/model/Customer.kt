package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.PaymentSystem

class Customer(
    val id: Long,
    val name: String,
    val billingKeys: MutableList<CustomerPaymentBillingKey>
) {
    fun addBillingKey(paymentSystem: PaymentSystem, billingKey: String) {
        billingKeys.add(CustomerPaymentBillingKey(this.id, paymentSystem, billingKey))
    }
}