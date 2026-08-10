package com.coco.payment.persistence.model

import java.time.Instant
import org.apache.ibatis.type.Alias
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus

@Alias("payment_transaction")
data class PaymentTransaction(
    var id: Long?,
    val paymentKey: String,
    val orderSeq: Long,
    val moid: String,
    val tid: String?,
    val amount: Long,
    val status: PaymentTransactionStatus,
    val expiredAt: Instant,
    var createdAt: Instant?,
    var updatedAt: Instant?,
)
