package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.CancelStatus
import org.apache.ibatis.type.Alias
import java.time.Instant

@Alias("payment_cancel")
data class PaymentCancel(
    var id: Long?,
    val paymentTransactionSeq: Long,
    val reason: String,
    val status: CancelStatus,
    val transactionKey: String?,
    val lastError: String?,
    val canceledAt: Instant?,
    var createdAt: Instant?,
    var updatedAt: Instant?,
)
