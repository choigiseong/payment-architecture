package com.coco.payment.persistence.model

import java.time.Instant
import org.apache.ibatis.type.Alias

@Alias("order")
data class Order(
    var id: Long?,
    val companySeq: Long,
    val totalPrice: Long,
    var createdAt: Instant?,
    var updatedAt: Instant?,
)
