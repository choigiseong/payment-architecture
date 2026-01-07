package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.enumerator.RefundAttemptStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

@Entity
@Table(name = "refund_attempt")
class RefundAttempt(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var invoiceSeq: Long,
//    @Column(nullable = true)
//    var pgTransactionKey: String?,
//    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var pgTransactionKey: String? = null, // '이번 환불'의 고유 키
    @Column(nullable = false)
    var amount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RefundAttemptStatus = RefundAttemptStatus.PENDING,
    @Column(nullable = false)
    var reason: String,
    @Column(nullable = false)
    var requestedAt: Instant,
    @Column(nullable = true)
    var canceledAt: Instant? = null,
    @Column(nullable = true)
    var failedAt: Instant? = null,
    @Column(nullable = true)
    var failureCode: String? = null,
    @Column(nullable = true)
    var failureReason: String? = null,
    @Column(nullable = false)
    var createdAt: Instant = Instant.now()
)
