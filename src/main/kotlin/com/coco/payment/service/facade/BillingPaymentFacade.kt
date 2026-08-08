package com.coco.payment.service.facade

import com.coco.payment.handler.paymentgateway.toss.TossBillingPaymentHandler
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentCommand
import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import com.coco.payment.service.BillingPaymentService
import com.coco.payment.service.OrderService
import com.coco.payment.service.PaymentWorkflowService
import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.service.dto.BillingPaymentResult
import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import org.springframework.stereotype.Service

@Service
class BillingPaymentFacade(
    private val orderService: OrderService,
    private val billingPaymentService: BillingPaymentService,
    private val paymentWorkflowService: PaymentWorkflowService,
    private val tossBillingPaymentHandler: TossBillingPaymentHandler,
) {
    fun pay(command: BillingPaymentCommand): BillingPaymentResult {
        val existingTransaction = billingPaymentService.findByPaymentKey(command.paymentKey)
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
        val result = tossBillingPaymentHandler.approve(
            TossBillingPaymentCommand(prepared.billingKey, prepared.customerKey, prepared.moid, prepared.orderName, prepared.amount)
        )

        return when (result) {
            is PaymentResult.Success -> {
                paymentWorkflowService.complete(prepared, result.value)
                BillingPaymentResult(prepared.orderKey, prepared.paymentKey, OrderStatus.PAID, PaymentTransactionStatus.SUCCESS, result.value.tid, null, null)
            }
            is PaymentResult.Failure -> {
                paymentWorkflowService.fail(prepared)
                BillingPaymentResult(prepared.orderKey, prepared.paymentKey, OrderStatus.PAYMENT_FAILED, PaymentTransactionStatus.FAILED, null, result.error.code, result.error.message)
            }
            is PaymentResult.Unknown -> {
                paymentWorkflowService.fail(prepared)
                BillingPaymentResult(prepared.orderKey, prepared.paymentKey, OrderStatus.PAYMENT_FAILED, PaymentTransactionStatus.FAILED, null, result.error.code, result.error.message)
            }
        }
    }

    fun poll(paymentKey: String): BillingPaymentResult? {
        val transaction = billingPaymentService.findByPaymentKey(paymentKey) ?: return null
        val order = orderService.findById(transaction.orderSeq) ?: return null
        return BillingPaymentResult(order.orderKey, transaction.paymentKey, order.status, transaction.status, transaction.tid, null, null)
    }
}
