package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.PaymentTransaction
import org.apache.ibatis.annotations.Param

interface PaymentTransactionRepository {
    fun insert(paymentTransaction: PaymentTransaction): Int

    fun findById(@Param("id") id: Long): PaymentTransaction?
}
