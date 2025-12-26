package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.PaymentAttemptStatus
import com.coco.payment.persistence.model.PaymentAttempt
import org.hibernate.annotations.processing.SQL
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface PaymentAttemptRepository : JpaRepository<PaymentAttempt, Long> {


    @SQL(
        """
            SELECT * FROM payment_attempt
            WHERE requested_at < :at AND status = :pending
        """
    )
    fun findByStatus(at: Instant, pending: PaymentAttemptStatus): List<PaymentAttempt>

    @SQL(
        "UPDATE payment_attempt SET " +
                "pg_transaction_key = :pgTransactionKey, " +
                "status = :toStatus, " +
                "approved_at = :approvedAt " +
                "WHERE invoice_seq = :invoiceSeq AND status IN (:fromStatus)"
    )
    fun succeeded(
        invoiceSeq: Long,
        pgTransactionKey: String,
        fromStatus: Set<PaymentAttemptStatus>,
        toStatus: PaymentAttemptStatus,
        approvedAt: Instant,
    ): Long


    @SQL(
        "UPDATE payment_attempt SET " +
                "status = :toStatus, " +
                "failed_reason = :failedReason, " +
                "failed_at = :failedAt " +
                "WHERE invoice_seq = :invoiceSeq AND status IN (:fromStatus)"
    )
    fun failed(
        invoiceSeq: Long,
        fromStatus: Set<PaymentAttemptStatus>,
        toStatus: PaymentAttemptStatus,
        failedReason: String,
        failedAt: Instant
    ): Long

}
