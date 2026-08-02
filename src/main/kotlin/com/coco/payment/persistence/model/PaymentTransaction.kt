package com.coco.payment.persistence.model

import java.time.Instant
import org.apache.ibatis.type.Alias

@Alias("payment_transaction")
data class PaymentTransaction(
    var id: Long?,
    val orderSeq: Long,
    val moid: String,
    val tid: String?,
    val amount: Long,
    val status: String,
    var createdAt: Instant?,
    var updatedAt: Instant?,
)
