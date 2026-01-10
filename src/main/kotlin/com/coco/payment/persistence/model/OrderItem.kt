package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.OrderItemStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "order_item")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val orderSeq: Long, // 주문 ID (Invoice와 연결될 수 있음)

    @Column(nullable = false)
    val productId: Long,

    @Column(nullable = false)
    val productName: String,

    @Column(nullable = false)
    val amount: Long, // 아이템 가격

    @Column(nullable = false)
    val quantity: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderItemStatus = OrderItemStatus.ORDERED,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)