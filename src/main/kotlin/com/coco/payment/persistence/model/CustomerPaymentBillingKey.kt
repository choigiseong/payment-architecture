package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.PaymentSystem
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "customer_payment_billing_key")
class CustomerPaymentBillingKey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: Customer,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentSystem: PaymentSystem,
    @Column(nullable = false)
    var billingKey: String,
    @Column(nullable = false)
    var cardNumber: String,
    @Column(nullable = false)
    var cardCompany: String,
)
