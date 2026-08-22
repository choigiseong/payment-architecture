package com.coco.payment.persistence.model

import java.time.Instant
import java.time.LocalDate
import org.apache.ibatis.type.Alias
import com.coco.payment.persistence.enumerator.OrderStatus

@Alias("order")
data class Order(
    var id: Long?,
    val orderKey: String,
    val companySeq: Long,
    val totalPrice: Long,
    val deliveryDate: LocalDate,
    val status: OrderStatus,
    var createdAt: Instant?,
    var updatedAt: Instant?,
) {
    // 승인을 더 받을 수 있는 상태인가. 결제된 상태를 나열하면 상태를 늘릴 때마다 빠뜨리므로
    // (PREPARING이 그랬다) 아직 결제 전인지로 판단한다.
    val acceptsPayment: Boolean get() = status == OrderStatus.PENDING_PAYMENT
}
