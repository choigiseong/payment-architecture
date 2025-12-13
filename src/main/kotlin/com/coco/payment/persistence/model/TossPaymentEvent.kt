package com.coco.payment.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "toss_payment_event")
class TossPaymentEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var customerSeq: String = "",
    @Column(nullable = false)
    var paymentKey: String = "",
    @Column(nullable = false)
    var orderId: String = "",
    @Column(nullable = false)
    var totalAmount: Long = 0,
    @Column(nullable = false)
    var balanceAmount: Long = 0,
    @Column(nullable = false)
    var status: String = "",
    @Column(nullable = false)
    var requestedAt: Instant = Instant.now(),
    @Column(nullable = false)
    var approvedAt: Instant = Instant.now(),
    @Column(nullable = false)
    var taxFreeAmount: Long = 0
)
