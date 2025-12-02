package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.PaymentSystem

class Customer(
    val id: Long,
    val name: String,
    val billingKeys: MutableList<CustomerPaymentBillingKey>
) {
    fun addBillingKey(paymentSystem: PaymentSystem, billingKey: String, cardNumber: String, cardCompany: String) {
        billingKeys.add(CustomerPaymentBillingKey(this.id, paymentSystem, billingKey, cardNumber, cardCompany))
    }

    fun getBillingKey(paymentSystem: PaymentSystem): CustomerPaymentBillingKey? {
        return billingKeys.find { it.paymentSystem == paymentSystem }
    }

    fun getCustomerKey(): String {
        // 이거 id를 대칭키 암호화하고 싶은데
        return "customer-${this.id}"
    }
}