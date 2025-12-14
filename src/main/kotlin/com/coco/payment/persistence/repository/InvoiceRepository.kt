package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.Invoice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface InvoiceRepository : JpaRepository<Invoice, Long> {
    fun findBySubscriptionSeq(subscriptionSeq: Long): List<Invoice>
    fun findBySubscriptionSeqAndExternalOrderKey(subscriptionSeq: Long, externalOrderKey: String): Invoice?
    fun findBySubscriptionSeqAndPeriodStart(id: Long, periodStart: LocalDate): Invoice?
}
