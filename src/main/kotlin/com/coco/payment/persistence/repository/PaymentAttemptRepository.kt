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
        "UPDATE payment_attempt SET " +
                "pg_transaction_key = :pgTransactionKey, " +
                "status = :toStatus, " +
                "approved_at = :approvedAt " +
                "WHERE invoice_seq = :invoiceSeq AND status IN (:fromStatus)"
    )
    fun success(
        invoiceSeq: Long,
        pgTransactionKey: String,
        fromStatus: Set<PaymentAttemptStatus>,
        toStatus: PaymentAttemptStatus,
        approvedAt: Instant,
    ): Long
}
