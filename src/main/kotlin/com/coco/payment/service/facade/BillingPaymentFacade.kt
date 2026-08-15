package com.coco.payment.service.facade

import com.coco.payment.handler.paymentgateway.toss.TossBillingPaymentHandler
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentCommand
import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import com.coco.payment.service.OrderService
import com.coco.payment.service.PaymentWorkflowService
import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.service.dto.BillingPaymentResult
import com.coco.payment.service.dto.PrepareBillingPaymentResult
import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import org.springframework.stereotype.Service

@Service
class BillingPaymentFacade(
    private val orderService: OrderService,
    private val paymentWorkflowService: PaymentWorkflowService,
    private val tossBillingPaymentHandler: TossBillingPaymentHandler,
) {
    fun pay(command: BillingPaymentCommand): BillingPaymentResult {
        val existingTransaction = paymentWorkflowService.findByPaymentKey(command.paymentKey)
        if (existingTransaction != null) {
            val order = orderService.findById(existingTransaction.orderSeq)
                ?: throw IllegalStateException("Order not found for payment transaction: ${existingTransaction.id}")
            return BillingPaymentResult(
                order.orderKey,
                existingTransaction.paymentKey,
                order.status,
                existingTransaction.status,
                existingTransaction.tid,
                null,
                null,
            )
        }

        val prepared = paymentWorkflowService.prepare(command)
        if (prepared is PrepareBillingPaymentResult.AlreadyPending) {
            val order = orderService.findById(prepared.orderId)
                ?: throw IllegalStateException("Order not found for payment transaction with paymentKey: ${prepared.paymentKey}")
            return BillingPaymentResult(order.orderKey, prepared.paymentKey, order.status, prepared.status, prepared.tid, null, null)
        }
        val result = prepared as PrepareBillingPaymentResult.Ready

        val approveResult = tossBillingPaymentHandler.approve(
            TossBillingPaymentCommand(result.billingKey, result.customerKey, result.moid, result.orderName, result.amount)
        )

        return when (approveResult) {
            is PaymentResult.Success -> {
                paymentWorkflowService.complete(result, approveResult.value)
                BillingPaymentResult(result.orderKey, result.paymentKey, OrderStatus.PAID, PaymentTransactionStatus.SUCCESS, approveResult.value.tid, null, null)
            }
            is PaymentResult.Failure -> {
                paymentWorkflowService.fail(result)
                // 이번 시도만 실패했을 뿐 주문은 아직 미결제 상태다(같은 orderKey로 재시도 가능).
                BillingPaymentResult(result.orderKey, result.paymentKey, OrderStatus.PENDING_PAYMENT, PaymentTransactionStatus.FAILED, null, approveResult.error.code, approveResult.error.message)
            }
            is PaymentResult.Unknown ->
                BillingPaymentResult(result.orderKey, result.paymentKey, OrderStatus.PENDING_PAYMENT, PaymentTransactionStatus.PENDING, null, approveResult.error.code, approveResult.error.message)
        }
    }

    fun poll(paymentKey: String): BillingPaymentResult? {
        val transaction = paymentWorkflowService.findByPaymentKey(paymentKey) ?: return null
        val order = orderService.findById(transaction.orderSeq) ?: return null
        return BillingPaymentResult(order.orderKey, transaction.paymentKey, order.status, transaction.status, transaction.tid, null, null)
    }
}
