package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.PaymentSystem
import org.apache.ibatis.type.Alias
import java.time.Instant

@Alias("company_billing_key")
data class CompanyBillingKey(
    var id: Long?,
    val companySeq: Long,
    val paymentSystem: PaymentSystem,
    val customerKey: String,
    val billingKey: String,
    var createdAt: Instant?,
    var updatedAt: Instant?,
)
