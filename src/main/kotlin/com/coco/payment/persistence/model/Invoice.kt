package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.InvoiceStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "invoice")
class Invoice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var subscriptionSeq: Long,
    @Column(nullable = false)
    var periodStart: LocalDate,
    @Column(nullable = false)
    var periodEnd: LocalDate,
    @Column(nullable = false)
    var amount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InvoiceStatus = InvoiceStatus.PENDING,
    @Column(nullable = true)
    var dueAt: Instant? = null, // todo 필요할까?
    @Column(nullable = true)
    var paidAt: Instant? = null,
    @Column(nullable = true)
    var lastAttemptAt: Instant? = null,
    @Column(nullable = false, unique = true)
    var externalOrderKey: String,
    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun isPaid(): Boolean {
        return status.isPaid()
    }

    companion object {
        fun buildExternalOrderKey(consumerSeq: Long, subscriptionSeq: Long, periodStart: LocalDate, periodEnd: LocalDate): String {
            return "invoice-${consumerSeq}-${subscriptionSeq}-${periodStart}-${periodEnd}"
        }
    }
}
