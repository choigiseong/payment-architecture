package com.coco.payment.persistence.model

import java.time.Instant
import org.apache.ibatis.type.Alias

@Alias("order_item")
data class OrderItem(
    var id: Long?,
    val orderSeq: Long,
    val itemName: String,
    val unitPrice: Long,
    val quantity: Int,
    var createdAt: Instant?,
    var updatedAt: Instant?,
)
