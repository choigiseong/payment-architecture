package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.InvoiceStatus
import com.coco.payment.persistence.enumerator.RefundAttemptStatus
import com.coco.payment.persistence.model.RefundAttempt
import org.hibernate.annotations.processing.SQL
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface RefundAttemptRepository : JpaRepository<RefundAttempt, Long> {

    @SQL(
        """
            SELECT SUM(amount) FROM refund_attempt
            WHERE invoice_seq = :invoiceSeq AND status = :status
        """
    )
    fun sumSuccessAmountByInvoice(invoiceSeq: Long, status: RefundAttemptStatus): Long

    @SQL(
        "UPDATE refund_attempt SET " +
                "pg_transaction_key = :pgTransactionKey, " +
                "status = :toStatus, " +
                "canceled_at = :canceledAt " +
                "WHERE invoice_seq = :invoiceSeq AND status IN (:fromStatus)"
    )
    fun succeeded(
        invoiceSeq: Long,
        pgTransactionKey: String,
        fromStatus: Set<RefundAttemptStatus>,
        toStatus: RefundAttemptStatus,
        canceledAt: Instant
    ): Long
}