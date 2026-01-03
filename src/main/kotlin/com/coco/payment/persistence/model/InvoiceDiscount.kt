package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.DiscountType
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
@Table(name = "invoice_discount")
class InvoiceDiscount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val invoiceSeq: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: DiscountType,

    // 사용된 쿠폰 seq 혹은 포인트 이력 seq를 저장
    @Column(nullable = true)
    val refSeq: Long?,

    @Column(nullable = false)
    val amount: Long,

    @Column(nullable = false)
    val reason: String,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)