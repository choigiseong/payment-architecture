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

    fun findById(id: Long): Invoice {
        return invoiceRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Invoice not found") }
    }

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
        at: Instant,
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
            externalOrderKey = externalOrderKey,
            lastAttemptAt = at,
        )
    }

    fun findByExternalKey(
        externalOrderKey: String
    ): Invoice {
        return invoiceRepository.findByExternalOrderKey(externalOrderKey)
            ?: throw IllegalArgumentException("Invoice not found")
    }


    @Transactional
    fun handleRetryOrFinalFailed(id: Long, at: Instant) {
        updateLastAttemptAt(id, at)
        val invoice = findById(id)
        if (invoice.attemptCount >= maxRetry) {
            failed(id, at)
        }
    }

    @Transactional
    fun updateLastAttemptAt(id: Long, at: Instant) {
        val affectedRows = invoiceRepository.incrementAttempt(id, at, setOf(InvoiceStatus.PENDING))

        if (affectedRows != 1L) {
            throw IllegalArgumentException("Invoice not found")
        }
    }


    @Transactional
    fun failed(id: Long, at: Instant) {
        val affectedRows = invoiceRepository.failed(
            id,
            at,
            setOf(InvoiceStatus.PENDING),
            InvoiceStatus.FAILED
        )

        if (affectedRows != 1L) {
            throw IllegalArgumentException("Invoice not found")
        }
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


    companion object {
        private val maxRetry = 3
    }
}
