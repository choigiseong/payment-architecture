package com.coco.payment.service

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.model.Order
import com.coco.payment.persistence.model.OrderItem
import com.coco.payment.persistence.repository.OrderItemRepository
import com.coco.payment.persistence.repository.OrderRepository
import com.coco.payment.service.dto.BillingOrderItem
import org.springframework.stereotype.Service

@Service
class OrderService(private val orderRepository: OrderRepository, private val orderItemRepository: OrderItemRepository) {
    fun createPendingOrder(companySeq: Long, totalPrice: Long, items: List<BillingOrderItem>): Long {
        val order = Order(null, companySeq, totalPrice, OrderStatus.PENDING_PAYMENT, null, null)
        check(orderRepository.insert(order) == 1) { "Failed to insert order" }
        items.forEach { item ->
            check(orderItemRepository.insert(OrderItem(null, order.id!!, item.itemName, item.unitPrice, item.quantity, null, null)) == 1) {
                "Failed to insert order item"
            }
        }
        return order.id!!
    }

    fun markPaid(orderId: Long) {
        check(orderRepository.updateStatus(orderId, OrderStatus.PAID) == 1) { "Failed to mark order as paid" }
    }
}
