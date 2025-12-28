package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.InvoiceStatus
import com.coco.payment.persistence.model.Invoice
import org.hibernate.annotations.processing.SQL
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate

@Repository
interface InvoiceRepository : JpaRepository<Invoice, Long> {
    fun findBySubscriptionSeq(subscriptionSeq: Long): List<Invoice>
    fun findBySubscriptionSeqAndExternalOrderKey(subscriptionSeq: Long, externalOrderKey: String): Invoice?
    fun findBySubscriptionSeqAndPeriodStart(subscriptionSeq: Long, periodStart: LocalDate): Invoice?
    fun findByExternalOrderKey(externalOrderKey: String): Invoice?

    @SQL(
        "UPDATE invoice SET " +
                "paid_at = :paidAt, " +
                "status = :toStatus WHERE id = :id AND status IN (:fromStatus)"
    )
    fun paid(id: Long, paidAt: Instant, fromStatus: Set<InvoiceStatus>, toStatus: InvoiceStatus): Long

    @SQL(
        "UPDATE invoice SET " +
                "failed_at = :failedAt, " +
                "status = :toStatus WHERE id = :id AND status IN (:fromStatus)"
    )
    fun failed(id: Long, failedAt: Instant, fromStatus: Set<InvoiceStatus>, toStatus: InvoiceStatus): Long


    @SQL(
        "UPDATE invoice SET " +
                "status = :toStatus WHERE id = :id AND status IN (:fromStatus)"
    )
    fun refunded(id: Long, fromStatus: Set<InvoiceStatus>, toStatus: InvoiceStatus): Long

    @SQL(
        "UPDATE invoice SET " +
                "status = :toStatus WHERE id = :id AND status IN (:fromStatus)"
    )
    fun partiallyRefunded(id: Long, fromStatus: Set<InvoiceStatus>, toStatus: InvoiceStatus): Long


    @SQL(
        "UPDATE invoice SET " +
                "attempt_count = attempt_count + 1, " +
                "last_attempt_at = :at " +
                "WHERE id = :id AND status IN (:fromStatus)"
    )
    fun incrementAttempt(id: Long, at: Instant, fromStatus: Set<InvoiceStatus>): Long


}
