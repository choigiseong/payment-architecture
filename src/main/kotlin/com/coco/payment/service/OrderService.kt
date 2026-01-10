package com.coco.payment.service

import com.coco.payment.persistence.enumerator.OrderItemStatus
import com.coco.payment.persistence.repository.OrderItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val orderItemRepository: OrderItemRepository
) {

    @Transactional
    fun refundOrderItem(orderItemSeq: Long) {
        val affected = orderItemRepository.updateStatus(
            orderItemSeq,
            setOf(OrderItemStatus.ORDERED, OrderItemStatus.PARTIALLY_REFUNDED),
            OrderItemStatus.REFUNDED
        )
        
        if (affected != 1) {
            // 이미 환불되었거나 상태가 맞지 않는 경우
            // throw IllegalStateException("OrderItem status update failed for id: $orderItemSeq")
        }
    }
}