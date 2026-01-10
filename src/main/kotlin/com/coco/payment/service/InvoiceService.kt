package com.coco.payment.service

import com.coco.payment.persistence.enumerator.BillingCycle
import com.coco.payment.persistence.enumerator.InvoiceStatus
import com.coco.payment.persistence.enumerator.InvoiceType
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.Invoice
import com.coco.payment.persistence.repository.InvoiceRepository
import com.coco.payment.service.dto.PrepaymentView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
) {

    fun createPrepaymentInvoice(
        paymentSystem: PaymentSystem,
        customerSeq: Long,
        orderSeq: Long,
        paymentSummary: PrepaymentView.PaymentSummary,
        uuid: UUID,
        at: Instant,
    ): Invoice {
        val externalOrderKey = Invoice.buildExternalOrderKey(
            customerSeq,
            InvoiceType.PREPAYMENT,
            orderSeq,
            uuid
        )
        val invoice = Invoice.ofPrepayment(
            orderSeq = orderSeq,
            totalAmount = paymentSummary.totalAmount,
            paidAmount = paymentSummary.paidAmount,
            totalDiscount = paymentSummary.totalDiscount,
            paymentSystem = paymentSystem,
            externalOrderKey = externalOrderKey,
            at = at,
        )
        return invoiceRepository.save(invoice)
    }

    // 연체가 되어, 다음날 결제되어도 이전 결제일을 기반으로 구독 된다. 예 chatgpt
    fun findOrCreateCurrentSubscriptionInvoice(
        paymentSystem: PaymentSystem,
        customerSeq: Long,
        subscriptionSeq: Long,
        nextBillingDate: LocalDate,
        cycle: BillingCycle,
        amount: Long,
        uuid: UUID,
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
            customerSeq,
            InvoiceType.SUBSCRIPTION,
            subscriptionSeq,
            uuid,
        )
        val invoice = Invoice.ofSubscription(
            subscriptionSeq = subscriptionSeq,
            totalAmount = amount,
            periodStart = periodStart,
            periodEnd = periodEnd,
            paymentSystem = paymentSystem,
            externalOrderKey = externalOrderKey,
            at = at,
        )

        return invoiceRepository.save(invoice)
    }

    fun findById(id: Long): Invoice {
        return invoiceRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Invoice not found") }
    }

    fun findByIdWithLock(id: Long): Invoice {
        return invoiceRepository.findByIdWithLock(id)
            .orElseThrow { IllegalArgumentException("Invoice not found") }
    }

    fun findInvoiceBySubscriptionSeq(subscriptionSeq: Long): List<Invoice> {
        return invoiceRepository.findBySubscriptionSeq(subscriptionSeq)
    }

    fun findByExternalOrderKey(
        externalOrderKey: String
    ): Invoice {
        return invoiceRepository.findByExternalOrderKey(externalOrderKey)
            ?: throw IllegalArgumentException("Invoice not found")
    }

    fun findByOrderSeq(orderSeq: Long): Invoice? {
        return invoiceRepository.findByOrderSeq(orderSeq)
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
    fun paid(id: Long, paidAt: Instant, pgTransactionKey: String) {
        val affectedRows = invoiceRepository.paid(
            id,
            paidAt,
            pgTransactionKey,
            setOf(InvoiceStatus.PENDING),
            InvoiceStatus.PAID
        )

        if (affectedRows != 1L) {
            throw IllegalArgumentException("Invoice not found")
        }
    }


    @Transactional
    fun refunded(id: Long) {
        val affectedRows = invoiceRepository.refunded(
            id,
            setOf(InvoiceStatus.PAID, InvoiceStatus.PARTIALLY_REFUNDED),
            InvoiceStatus.REFUNDED
        )
        if (affectedRows != 1L) {
            throw IllegalArgumentException("Invoice not found")
        }
    }

    @Transactional
    fun partiallyRefunded(id: Long) {
        val affectedRows = invoiceRepository.partiallyRefunded(
            id,
            setOf(InvoiceStatus.PAID, InvoiceStatus.PARTIALLY_REFUNDED),
            InvoiceStatus.PARTIALLY_REFUNDED
        )
        if (affectedRows != 1L) {
            throw IllegalArgumentException("Invoice not found")
        }
    }

    companion object {
        private val maxRetry = 3
    }
}
