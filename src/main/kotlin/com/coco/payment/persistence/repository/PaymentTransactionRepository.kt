package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.PaymentTransaction
import com.coco.payment.persistence.enumerator.PaymentFailCode
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import org.apache.ibatis.annotations.Param
import java.time.Instant

interface PaymentTransactionRepository {
    fun insert(paymentTransaction: PaymentTransaction): Int

    fun findById(@Param("id") id: Long): PaymentTransaction?

    fun findByPaymentKey(@Param("paymentKey") paymentKey: String): PaymentTransaction?

    fun findByOrderSeqAndStatus(
        @Param("orderSeq") orderSeq: Long,
        @Param("status") status: PaymentTransactionStatus,
    ): PaymentTransaction?

    fun findPendingDueForCheck(
        @Param("status") status: PaymentTransactionStatus,
        @Param("approveDoneBefore") approveDoneBefore: Instant,
    ): List<PaymentTransaction>

    fun mark(
        @Param("id") id: Long,
        @Param("fromStatus") fromStatus: PaymentTransactionStatus,
        @Param("toStatus") toStatus: PaymentTransactionStatus,
        @Param("tid") tid: String?,
        @Param("approvedAt") approvedAt: Instant?,
        @Param("failCode") failCode: PaymentFailCode?,
        @Param("failMessage") failMessage: String?,
    ): Int
}
