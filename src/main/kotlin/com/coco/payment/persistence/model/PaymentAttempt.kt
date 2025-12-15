package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.PaymentAttemptStatus
import com.coco.payment.persistence.enumerator.PaymentSystem
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// 신규 시도 시 insert
@Entity
@Table(name = "payment_attempt")
class PaymentAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var invoiceSeq: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentSystem: PaymentSystem,
    @Column(nullable = false)
    var pgTransactionKey: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentAttemptStatus = PaymentAttemptStatus.PENDING,
    @Column(nullable = true)
    var requestedAt: Instant,
    @Column(nullable = true)
    var approvedAt: Instant? = null,
    @Column(nullable = true)
    var failureCode: String? = null,
    @Column(nullable = true)
    var failureReason: String? = null,
    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
)
