package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentAttemptStatus
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.PaymentAttempt
import com.coco.payment.persistence.repository.PaymentAttemptRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class PaymentAttemptService(
    private val paymentAttemptRepository: PaymentAttemptRepository
) {

    fun createPaymentAttempt(
        invoiceSeq: Long,
        paymentSystem: PaymentSystem,
        requestedAt: Instant,
    ) {
        paymentAttemptRepository.save(
            PaymentAttempt(
                invoiceSeq = invoiceSeq,
                paymentSystem = paymentSystem,
                requestedAt = requestedAt
            )
        )
    }

    fun findPendingPaymentAttempts(at: Instant): List<PaymentAttempt> {
        val timeoutAt = at.minus(Duration.ofMinutes(PENDING_TIMEOUT_MINUTES))
        return paymentAttemptRepository.findByStatus(timeoutAt, PaymentAttemptStatus.PENDING)
    }

    @Transactional
    fun succeeded(invoiceSeq: Long, approvedAt: Instant, pgTransactionKey: String) {
        val affectedRows = paymentAttemptRepository.succeeded(
            invoiceSeq,
            pgTransactionKey,
            setOf(PaymentAttemptStatus.PENDING),
            PaymentAttemptStatus.SUCCEEDED,
            approvedAt
        )
        if (affectedRows != 1L) {
            throw IllegalArgumentException("Payment attempt not found")
        }
    }

    @Transactional
    fun failed(invoiceSeq: Long, failedAt: Instant, failedReason: String) {
        val affectedRows = paymentAttemptRepository.failed(
            invoiceSeq,
            setOf(PaymentAttemptStatus.PENDING),
            PaymentAttemptStatus.FAILED,
            failedReason,
            failedAt
        )
        if (affectedRows != 1L) {
            throw IllegalArgumentException("Payment attempt not found")
        }
    }


    companion object {
        private const val PENDING_TIMEOUT_MINUTES = 1L
    }
}