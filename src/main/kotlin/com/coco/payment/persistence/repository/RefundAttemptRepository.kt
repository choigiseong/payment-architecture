package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.InvoiceStatus
import com.coco.payment.persistence.enumerator.RefundAttemptStatus
import com.coco.payment.persistence.model.RefundAttempt
import org.hibernate.annotations.processing.SQL
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface RefundAttemptRepository : JpaRepository<RefundAttempt, Long> {

    @Query(
        value = """
            SELECT COALESCE(SUM(amount), 0) FROM refund_attempt
            WHERE invoice_seq = :invoiceSeq AND status IN (:statuses)
        """,
        nativeQuery = true
    )
    fun sumAmountByInvoiceAndStatusIn(invoiceSeq: Long, statuses: List<String>): Long

    @Modifying
    @Query(
        value = """
            UPDATE refund_attempt
            SET pg_transaction_key = :pgTransactionKey,
                status = :toStatus,
                canceled_at = :canceledAt
            WHERE invoice_seq = :invoiceSeq
              AND status IN (:fromStatus)
        """,
        nativeQuery = true
    )
    fun succeeded(
        invoiceSeq: Long,
        pgTransactionKey: String,
        fromStatus: Set<RefundAttemptStatus>,
        toStatus: RefundAttemptStatus,
        canceledAt: Instant
    ): Long
}