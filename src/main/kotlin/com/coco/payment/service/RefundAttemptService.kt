package com.coco.payment.service

import com.coco.payment.persistence.enumerator.RefundAttemptStatus
import com.coco.payment.persistence.model.RefundAttempt
import com.coco.payment.persistence.repository.RefundAttemptRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class RefundAttemptService(
    private val refundAttemptRepository: RefundAttemptRepository,
) {

    fun createAttempt(
        invoiceSeq: Long,
        amount: Long,
        reason: String,
        at: Instant
    ) {
        refundAttemptRepository.save(
            RefundAttempt(
                invoiceSeq = invoiceSeq,
                amount = amount,
                reason = reason,
                requestedAt = at,
            )
        )

    }

    fun sumSuccessAmountByInvoice(invoiceSeq: Long): Long {
        return refundAttemptRepository.sumSuccessAmountByInvoice(invoiceSeq, RefundAttemptStatus.PENDING)
    }

    @Transactional
    fun succeeded(invoiceSeq: Long, canceledAt: Instant, pgTransactionKey: String) {
        val affected = refundAttemptRepository.succeeded(
            invoiceSeq,
            pgTransactionKey,
            setOf(RefundAttemptStatus.PENDING),
            RefundAttemptStatus.SUCCEEDED,
            canceledAt
        )
        if (affected != 1L) {
            throw IllegalArgumentException("Refund attempt not found")
        }
    }

}