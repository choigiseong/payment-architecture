package com.coco.payment.service

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.model.Order
import com.coco.payment.persistence.model.OrderItem
import com.coco.payment.persistence.repository.OrderItemRepository
import com.coco.payment.persistence.repository.OrderRepository
import com.coco.payment.service.dto.BillingOrderItem
import com.coco.payment.service.dto.BillingPaymentItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productService: ProductService,
    @Value("\${payment.delivery.cutoff-hour}")
    private val deliveryCutoffHour: Int,
) {
    // 이름과 가격은 클라이언트 값을 받지 않고 서버가 조회한 상품에서 가져온다.
    fun resolveOrderItems(items: List<BillingPaymentItem>): List<BillingOrderItem> {
        val productsById = productService.findByIds(items.map { it.productId })
        return items.map { item ->
            val product = productsById[item.productId]
                ?: throw IllegalArgumentException("Unknown product: ${item.productId}")
            BillingOrderItem(product.name, product.price, item.quantity)
        }
    }

    // 마감(22시) 전 주문은 오늘 배치를 타서 내일 도착, 이후 주문은 다음 배치라 모레 도착.
    fun computeDeliveryDate(): LocalDate {
        val now = LocalDateTime.now(SEOUL)
        val daysToAdd = if (now.hour < deliveryCutoffHour) 1L else 2L
        return now.toLocalDate().plusDays(daysToAdd)
    }

    fun findByOrderKey(orderKey: String) = orderRepository.findByOrderKey(orderKey)
    fun findByOrderKeyForUpdate(orderKey: String) = orderRepository.findByOrderKeyForUpdate(orderKey)
    fun findById(orderId: Long) = orderRepository.findById(orderId)

    fun findItems(orderId: Long): List<BillingOrderItem> =
        orderItemRepository.findByOrderSeq(orderId).map { BillingOrderItem(it.itemName, it.unitPrice, it.quantity) }

    fun createPendingOrder(orderKey: String, companySeq: Long, totalPrice: Long, deliveryDate: LocalDate, items: List<BillingOrderItem>): Long {
        val order = Order(null, orderKey, companySeq, totalPrice, deliveryDate, OrderStatus.PENDING_PAYMENT, null, null)
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

    @Transactional
    fun updateDeliveryDate(orderId: Long, deliveryDate: LocalDate) {
        check(orderRepository.updateDeliveryDate(orderId, deliveryDate) == 1) { "Failed to update delivery date" }
    }

    companion object {
        private val SEOUL = ZoneId.of("Asia/Seoul")
    }
}
