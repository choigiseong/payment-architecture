package com.coco.payment.service

import com.coco.payment.service.dto.BillingOrderItem
import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.service.dto.BillingPaymentItem
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentResult
import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.repository.CompanyBillingKeyRepository
import com.coco.payment.service.dto.PrepareBillingPaymentResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PaymentWorkflowService(
    private val orderService: OrderService,
    private val paymentTransactionService: PaymentTransactionService,
    private val productService: ProductService,
    private val companyBillingKeyRepository: CompanyBillingKeyRepository,
    @Value("\${payment.pending-timeout-seconds}")
    private val pendingTimeoutSeconds: Long,
) {
    fun findByPaymentKey(paymentKey: String) = paymentTransactionService.findByPaymentKey(paymentKey)

    @Transactional
    fun prepare(command: BillingPaymentCommand): PrepareBillingPaymentResult {
        val orderItems = resolveOrderItems(command.items, command.totalPrice)

        val existingOrder = orderService.findByOrderKeyForUpdate(command.orderKey)
        val order = if (existingOrder != null) {
            require(existingOrder.companySeq == command.companySeq && existingOrder.totalPrice == command.totalPrice) {
                "Order key is already associated with a different order"
            }
            existingOrder
        } else {
            val orderId = orderService.createPendingOrder(command.orderKey, command.companySeq, command.totalPrice, orderItems)
            orderService.findById(orderId) ?: error("Created order not found: $orderId")
        }

        // PENDING 거래가 없어도 주문 자체가 이미 결제 완료일 수 있다(예: 폴링이 끝난 뒤 재처리가 확정한 경우).
        // 이때 같은 orderKey로 다시 들어오면 중복 승인이 되므로 주문 상태로 막는다.
        require(order.status != OrderStatus.PAID) { "Order is already paid: ${command.orderKey}" }

        val pending = paymentTransactionService.findPendingByOrderSeq(order.id!!)
        if (pending != null) {
            return PrepareBillingPaymentResult.AlreadyPending(order.id!!, command.orderKey, pending.paymentKey, pending.status, pending.tid)
        }

        val moid = command.paymentKey
        val expiredAt = Instant.now().plusSeconds(pendingTimeoutSeconds)
        val paymentTransactionId = paymentTransactionService.createPending(command.paymentKey, order.id!!, moid, command.totalPrice, expiredAt)
        val billingKey = companyBillingKeyRepository.findByCompanySeqAndPaymentSystem(command.companySeq, PaymentSystem.TOSS)
            ?: throw IllegalArgumentException("Toss billing key not found for company: ${command.companySeq}")
        return PrepareBillingPaymentResult.Ready(order.id!!, paymentTransactionId, command.orderKey, command.paymentKey, billingKey.billingKey, billingKey.customerKey, moid, command.orderName, command.totalPrice)
    }

    // 이름과 가격은 클라이언트 값을 받지 않고 서버가 조회한 상품에서 가져온다.
    // 클라이언트의 totalPrice는 "사용자가 확인한 금액"이므로, 재계산 값과 다르면 승인 전에 거부한다.
    private fun resolveOrderItems(items: List<BillingPaymentItem>, totalPrice: Long): List<BillingOrderItem> {
        val productsById = productService.findByIds(items.map { it.productId })
        val orderItems = items.map { item ->
            val product = productsById[item.productId]
                ?: throw IllegalArgumentException("Unknown product: ${item.productId}")
            BillingOrderItem(product.name, product.price, item.quantity)
        }
        val computedTotal = orderItems.sumOf { it.unitPrice * it.quantity }
        require(computedTotal == totalPrice) {
            "Total price mismatch: requested $totalPrice but computed $computedTotal"
        }
        return orderItems
    }

    @Transactional
    fun complete(prepared: PrepareBillingPaymentResult.Ready, result: TossBillingPaymentResult) {
        paymentTransactionService.complete(prepared.paymentTransactionId, result.tid)
        orderService.markPaid(prepared.orderId)
    }

    // 실패는 "이번 시도"의 결과일 뿐 주문의 종료 상태가 아니다. 같은 orderKey로 재시도할 수 있으므로
    // 주문은 결제될 때까지 PENDING_PAYMENT로 두고, 시도별 결과는 payment_transaction에만 남긴다.
    @Transactional
    fun fail(prepared: PrepareBillingPaymentResult.Ready) {
        paymentTransactionService.fail(prepared.paymentTransactionId)
    }

    @Transactional
    fun completeByTransactionId(paymentTransactionId: Long, tid: String) {
        val transaction = paymentTransactionService.findById(paymentTransactionId)
            ?: error("Payment transaction not found: $paymentTransactionId")
        paymentTransactionService.complete(paymentTransactionId, tid)
        orderService.markPaid(transaction.orderSeq)
    }

    @Transactional
    fun failByTransactionId(paymentTransactionId: Long) {
        paymentTransactionService.fail(paymentTransactionId)
    }
}
