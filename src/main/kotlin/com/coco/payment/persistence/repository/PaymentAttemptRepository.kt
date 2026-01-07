package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.PaymentAttemptStatus
import com.coco.payment.persistence.model.PaymentAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface PaymentAttemptRepository : JpaRepository<PaymentAttempt, Long> {


    @Query(
        value = """
            SELECT * FROM payment_attempt
            WHERE requested_at < :at AND status = :pending
        """,
        nativeQuery = true
    )
    fun findByStatus(at: Instant, pending: PaymentAttemptStatus): List<PaymentAttempt>

    @Modifying
    @Query(
        value = "UPDATE payment_attempt SET status = :toStatus, approved_at = :approvedAt WHERE invoice_seq = :invoiceSeq AND status IN (:fromStatus)",
        nativeQuery = true
    )
    fun succeeded(
        invoiceSeq: Long,
        fromStatus: Set<PaymentAttemptStatus>,
        toStatus: PaymentAttemptStatus,
        approvedAt: Instant,
    ): Long


    @Modifying
    @Query(
        value = "UPDATE payment_attempt SET status = :toStatus, failed_reason = :failedReason, failed_at = :failedAt WHERE invoice_seq = :invoiceSeq AND status IN (:fromStatus)",
        nativeQuery = true
    )
    fun failed(
        invoiceSeq: Long,
        fromStatus: Set<PaymentAttemptStatus>,
        toStatus: PaymentAttemptStatus,
        failedReason: String,
        failedAt: Instant
    ): Long

    @Query(
        value = """
            SELECT * FROM payment_attempt
            WHERE invoice_seq = :invoiceSeq AND status = :status
            ORDER BY approved_at DESC
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findFirstByInvoiceSeqAndStatusOrderByApprovedAtDesc(
        invoiceSeq: Long,
        status: PaymentAttemptStatus
    ): PaymentAttempt?

}
