package com.coco.payment.service

import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentResult
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.repository.CompanyBillingKeyRepository
import com.coco.payment.service.dto.PrepareBillingPaymentResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentWorkflowService(
    private val orderService: OrderService,
    private val paymentTransactionService: PaymentTransactionService,
    private val companyBillingKeyRepository: CompanyBillingKeyRepository,
) {
    fun findByPaymentKey(paymentKey: String) = paymentTransactionService.findByPaymentKey(paymentKey)

    // TODO: PENDING 거래의 PG 승인 결과 조회 및 만료·재처리 정책을 추가한다.

    @Transactional
    fun prepare(command: BillingPaymentCommand): PrepareBillingPaymentResult {
        val existingOrder = orderService.findByOrderKey(command.orderKey)
        val order = if (existingOrder != null) {
            require(existingOrder.companySeq == command.companySeq && existingOrder.totalPrice == command.totalPrice) {
                "Order key is already associated with a different order"
            }
            existingOrder
        } else {
            val orderId = orderService.createPendingOrder(command.orderKey, command.companySeq, command.totalPrice, command.items)
            orderService.findById(orderId) ?: error("Created order not found: $orderId")
        }
        val moid = command.paymentKey
        val paymentTransactionId = paymentTransactionService.createPending(command.paymentKey, order.id!!, moid, command.totalPrice)
        val billingKey = companyBillingKeyRepository.findByCompanySeqAndPaymentSystem(command.companySeq, PaymentSystem.TOSS)
            ?: throw IllegalArgumentException("Toss billing key not found for company: ${command.companySeq}")
        return PrepareBillingPaymentResult(order.id!!, paymentTransactionId, command.orderKey, command.paymentKey, billingKey.billingKey, billingKey.customerKey, moid, command.orderName, command.totalPrice)
    }

    @Transactional
    fun complete(prepared: PrepareBillingPaymentResult, result: TossBillingPaymentResult) {
        paymentTransactionService.complete(prepared.paymentTransactionId, result.tid)
        orderService.markPaid(prepared.orderId)
    }

    @Transactional
    fun fail(prepared: PrepareBillingPaymentResult) {
        paymentTransactionService.fail(prepared.paymentTransactionId)
        orderService.markPaymentFailed(prepared.orderId)
    }
}
