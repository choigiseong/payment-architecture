package com.coco.payment.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "refund_attempt_item")
class RefundAttemptItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val refundAttemptSeq: Long,

    @Column(nullable = false)
    val orderItemSeq: Long,

    @Column(nullable = false)
    val refundAmount: Long, // 해당 아이템에 대한 환불 금액

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)