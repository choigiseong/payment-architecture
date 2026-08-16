package com.coco.payment.persistence.model

import java.time.Instant
import org.apache.ibatis.type.Alias
import com.coco.payment.persistence.enumerator.OrderStatus

@Alias("order")
data class Order(
    var id: Long?,
    val orderKey: String,
    val companySeq: Long,
    val totalPrice: Long,
    val status: OrderStatus,
    var createdAt: Instant?,
    var updatedAt: Instant?,
) {
    val isPaid: Boolean get() = status == OrderStatus.PAID
}
