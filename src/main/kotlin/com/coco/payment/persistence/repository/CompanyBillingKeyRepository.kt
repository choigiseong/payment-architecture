package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.CompanyBillingKey
import org.apache.ibatis.annotations.Param

interface CompanyBillingKeyRepository {
    fun insert(companyBillingKey: CompanyBillingKey): Int

    fun findByCompanySeqAndPaymentSystem(
        @Param("companySeq") companySeq: Long,
        @Param("paymentSystem") paymentSystem: PaymentSystem,
    ): CompanyBillingKey?

    fun updateBillingKey(
        @Param("id") id: Long,
        @Param("billingKey") billingKey: String,
    ): Int
}
