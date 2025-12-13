package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.PaymentSystem
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "customer")
class Customer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var name: String,
    @OneToMany(
        mappedBy = "customer",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var billingKeys: MutableList<CustomerPaymentBillingKey> = mutableListOf()
) {
    fun addBillingKey(paymentSystem: PaymentSystem, billingKey: String, cardNumber: String, cardCompany: String) {
        billingKeys.add(CustomerPaymentBillingKey(null, this, paymentSystem, billingKey, cardNumber, cardCompany))
    }

    fun getBillingKey(paymentSystem: PaymentSystem): CustomerPaymentBillingKey? {
        return billingKeys.find { it.paymentSystem == paymentSystem }
    }

    fun getCustomerKey(): String {
        return "customer-${this.id}"
    }
}
