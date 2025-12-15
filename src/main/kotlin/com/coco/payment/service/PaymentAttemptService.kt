package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentAttemptStatus
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.PaymentAttempt
import com.coco.payment.persistence.repository.PaymentAttemptRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

    @Transactional
    fun success(invoiceSeq: Long, approvedAt: Instant, pgTransactionKey: String) {
        val affectedRows = paymentAttemptRepository.success(
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
}