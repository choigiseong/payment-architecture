package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.ClaimStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "claim")
class Claim(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val customerSeq: Long,

    @Column(nullable = false)
    val orderSeq: Long, // 어떤 주문에 대한 클레임인지

    @Column(nullable = false)
    val reason: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ClaimStatus = ClaimStatus.REQUESTED,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)