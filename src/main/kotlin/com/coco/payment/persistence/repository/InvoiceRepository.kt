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
}
