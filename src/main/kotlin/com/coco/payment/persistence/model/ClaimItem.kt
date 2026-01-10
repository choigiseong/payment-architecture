package com.coco.payment.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "claim_item")
class ClaimItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val claimSeq: Long,

    @Column(nullable = false)
    val orderItemSeq: Long,

    @Column(nullable = false)
    val quantity: Int, // 반품 수량

    @Column(nullable = false)
    val claimAmount: Long, // 반품 요청 금액 (배송비 제외 등 정책 적용 가능)

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)