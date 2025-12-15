package com.coco.payment.service

import com.coco.payment.persistence.enumerator.BillingCycle
import com.coco.payment.persistence.enumerator.InvoiceStatus
import com.coco.payment.persistence.model.Invoice
import com.coco.payment.persistence.repository.InvoiceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
) {

    fun findInvoiceBySubscriptionSeq(subscriptionSeq: Long): List<Invoice> {
        return invoiceRepository.findBySubscriptionSeq(subscriptionSeq)
    }

    // 연체가 되어, 다음날 결제되어도 이전 결제일을 기반으로 구독 된다. 예 chatgpt
    fun findOrCreateCurrent(
        consumerSeq: Long,
        subscriptionSeq: Long,
        nextBillingDate: LocalDate,
        cycle: BillingCycle,
        amount: Long,
    ): Invoice {
        val periodStart = nextBillingDate
        val periodEnd = cycle.getPeriodEnd(nextBillingDate)
        invoiceRepository
            .findBySubscriptionSeqAndPeriodStart(
                subscriptionSeq,
                periodStart
            )
            ?.let { return it }

        val externalOrderKey = Invoice.buildExternalOrderKey(
            consumerSeq,
            subscriptionSeq,
            periodStart,
            periodEnd
        )
        return Invoice(
            subscriptionSeq = subscriptionSeq,
            amount = amount,
            periodStart = periodStart,
            periodEnd = periodEnd,
            externalOrderKey = externalOrderKey
        )
    }

    fun findByExternalKey(
        externalOrderKey: String
    ): Invoice {
        return invoiceRepository.findByExternalOrderKey(externalOrderKey)
            ?: throw IllegalArgumentException("Invoice not found")
    }

    @Transactional
    fun paid(id: Long, paidAt: Instant) {
        val affectedRows = invoiceRepository.paid(
            id,
            paidAt,
            setOf(InvoiceStatus.PENDING),
            InvoiceStatus.PAID
        )

        if (affectedRows != 1L) {
            throw IllegalArgumentException("Invoice not found")
        }
    }
}
