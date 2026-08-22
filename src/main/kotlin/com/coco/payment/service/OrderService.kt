package com.coco.payment.service

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.model.Order
import com.coco.payment.persistence.model.OrderItem
import com.coco.payment.persistence.repository.OrderItemRepository
import com.coco.payment.persistence.repository.OrderRepository
import com.coco.payment.service.dto.BillingOrderItem
import com.coco.payment.service.dto.BillingPaymentItem
import com.coco.payment.support.Dates
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

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
        val now = Dates.now()
        val daysToAdd = if (now.hour < deliveryCutoffHour) 1L else 2L
        return now.toLocalDate().plusDays(daysToAdd)
    }

    // TODO: 전체를 한 번에 읽는다. 주문이 많아지면 id 기준 seek 페이징으로 나눠 조회한다.
    //  처리하면서 상태가 바뀌어 대상에서 빠지므로 offset 페이징은 건너뛰는 건을 만든다.
    fun findByDeliveryDateAndStatus(deliveryDate: LocalDate, status: OrderStatus) =
        orderRepository.findByDeliveryDateAndStatus(deliveryDate, status)

    fun findByOrderKey(orderKey: String) = orderRepository.findByOrderKey(orderKey)
    fun findByOrderKeyForUpdate(orderKey: String) = orderRepository.findByOrderKeyForUpdate(orderKey)
    fun findById(orderId: Long) = orderRepository.findById(orderId)

    // 합계가 같아도 상품 구성은 다를 수 있다(예: 6500x2 와 5000+3800+4200).
    fun hasSameItems(orderId: Long, items: List<BillingOrderItem>): Boolean =
        canonicalize(orderItemRepository.findByOrderSeq(orderId).map { BillingOrderItem(it.itemName, it.unitPrice, it.quantity) }) ==
            canonicalize(items)

    // 순서와 무관하게 비교하기 위한 정렬된 표현.
    private fun canonicalize(items: List<BillingOrderItem>) =
        items.map { "${it.itemName}:${it.unitPrice}:${it.quantity}" }.sorted()

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

    @Transactional
    fun markPreparing(orderId: Long) {
        check(orderRepository.mark(orderId, OrderStatus.PAID, OrderStatus.PREPARING) == 1) {
            "Failed to mark order as preparing"
        }
    }
}
