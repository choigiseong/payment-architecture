package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.DiscrepancyStatus
import com.coco.payment.persistence.enumerator.DiscrepancyType
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import com.coco.payment.persistence.enumerator.PgPaymentStatus
import org.apache.ibatis.type.Alias
import java.time.Instant

@Alias("reconciliation_discrepancy")
data class ReconciliationDiscrepancy(
    var id: Long?,
    val paymentSystem: PaymentSystem,
    val type: DiscrepancyType,
    val status: DiscrepancyStatus,
    val moid: String,
    val ourStatus: PaymentTransactionStatus?,
    val pgStatus: PgPaymentStatus?,
    val ourAmount: Long?,
    val pgAmount: Long?,
    val detail: String?,
    var createdAt: Instant?,
    var updatedAt: Instant?,
)
