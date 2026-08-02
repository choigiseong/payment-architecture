package com.coco.payment.persistence.mapper

import com.coco.payment.persistence.model.PaymentTransaction
import org.apache.ibatis.annotations.Param

interface PaymentTransactionMapper {
    fun insert(paymentTransaction: PaymentTransaction): Int

    fun findById(@Param("id") id: Long): PaymentTransaction?
}
