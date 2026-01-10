package com.coco.payment.service

import com.coco.payment.persistence.enumerator.RefundAttemptStatus
import com.coco.payment.persistence.model.RefundAttempt
import com.coco.payment.persistence.repository.InvoiceRepository
import com.coco.payment.persistence.repository.RefundAttemptRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class RefundAttemptService(
    private val refundAttemptRepository: RefundAttemptRepository,
    private val invoiceRepository: InvoiceRepository
) {

    @Transactional
    fun createAttemptIfRefundable(
        invoiceSeq: Long,
        requestAmount: Long,
        reason: String,
        at: Instant
    ) {
        // 비관적 락을 걸어 동시성 제어
        val invoice = invoiceRepository.findByIdWithLock(invoiceSeq)
            .orElseThrow { IllegalArgumentException("Invoice not found") }

        val alreadyRefunded = sumRefundedAndPendingAmount(invoiceSeq)
        val refundableAmount = invoice.paidAmount - alreadyRefunded

        if (requestAmount > refundableAmount) {
            throw IllegalStateException("환불 가능한 최대 금액은 $refundableAmount 원입니다.")
        }

        refundAttemptRepository.save(
            RefundAttempt(
                invoiceSeq = invoiceSeq,
                amount = requestAmount,
                reason = reason,
                requestedAt = at,
            )
        )
    }

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

    fun sumRefundedAndPendingAmount(invoiceSeq: Long): Long {
        // 성공한 환불뿐만 아니라, 현재 진행 중인(PENDING) 환불 시도 금액도 포함해야
        // 중복 환불 요청으로 인한 초과 환불을 방지할 수 있습니다.
        return refundAttemptRepository.sumAmountByInvoiceAndStatusIn(
            invoiceSeq,
            listOf(RefundAttemptStatus.SUCCEEDED, RefundAttemptStatus.PENDING)
        )
    }

    fun sumSucceededAmountByInvoice(invoiceSeq: Long): Long {
        return refundAttemptRepository.sumAmountByInvoiceAndStatusIn(
            invoiceSeq,
            listOf(RefundAttemptStatus.SUCCEEDED)
        )
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