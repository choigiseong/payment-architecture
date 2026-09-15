package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.CancelStatus
import com.coco.payment.persistence.model.PaymentCancel
import org.apache.ibatis.annotations.Param
import java.time.Instant

interface PaymentCancelRepository {
    fun insert(paymentCancel: PaymentCancel): Int

    fun findByStatus(@Param("status") status: CancelStatus): List<PaymentCancel>

    fun findByTransactionSeqAndStatus(
        @Param("paymentTransactionSeq") paymentTransactionSeq: Long,
        @Param("status") status: CancelStatus,
    ): PaymentCancel?

    fun markDone(
        @Param("id") id: Long,
        @Param("fromStatus") fromStatus: CancelStatus,
        @Param("toStatus") toStatus: CancelStatus,
        @Param("transactionKey") transactionKey: String,
        @Param("canceledAt") canceledAt: Instant,
    ): Int

    fun updateLastError(@Param("id") id: Long, @Param("lastError") lastError: String?): Int

    fun findByTransactionKey(@Param("transactionKey") transactionKey: String): PaymentCancel?

    fun findByStatusAndCreatedAtBetween(
        @Param("status") status: CancelStatus,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
    ): List<PaymentCancel>

    fun findByStatusAndCanceledAtBetween(
        @Param("status") status: CancelStatus,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
    ): List<PaymentCancel>
}
