package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.PaymentTransaction
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import org.apache.ibatis.annotations.Param

interface PaymentTransactionRepository {
    fun insert(paymentTransaction: PaymentTransaction): Int

    fun findById(@Param("id") id: Long): PaymentTransaction?

    fun findByPaymentKey(@Param("paymentKey") paymentKey: String): PaymentTransaction?

    fun mark(
        @Param("id") id: Long,
        @Param("fromStatus") fromStatus: PaymentTransactionStatus,
        @Param("toStatus") toStatus: PaymentTransactionStatus,
        @Param("tid") tid: String?,
    ): Int
}
