package com.coco.payment.service

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.model.Order
import com.coco.payment.persistence.model.OrderItem
import com.coco.payment.persistence.repository.OrderItemRepository
import com.coco.payment.persistence.repository.OrderRepository
import com.coco.payment.service.dto.BillingOrderItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(private val orderRepository: OrderRepository, private val orderItemRepository: OrderItemRepository) {
    fun findByOrderKey(orderKey: String) = orderRepository.findByOrderKey(orderKey)
    fun findByOrderKeyForUpdate(orderKey: String) = orderRepository.findByOrderKeyForUpdate(orderKey)
    fun findById(orderId: Long) = orderRepository.findById(orderId)

    fun findItems(orderId: Long): List<BillingOrderItem> =
        orderItemRepository.findByOrderSeq(orderId).map { BillingOrderItem(it.itemName, it.unitPrice, it.quantity) }

    fun createPendingOrder(orderKey: String, companySeq: Long, totalPrice: Long, items: List<BillingOrderItem>): Long {
        val order = Order(null, orderKey, companySeq, totalPrice, OrderStatus.PENDING_PAYMENT, null, null)
        check(orderRepository.insert(order) == 1) { "Failed to insert order" }
        items.forEach { item ->
            check(orderItemRepository.insert(OrderItem(null, order.id!!, item.itemName, item.unitPrice, item.quantity, null, null)) == 1) {
                "Failed to insert order item"
            }
        }
        return order.id!!
    }

    @Transactional
    fun markPaid(orderId: Long) {
        check(orderRepository.mark(orderId, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID) == 1) { "Failed to mark order as paid" }
    }
}
