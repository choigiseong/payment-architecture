package com.coco.payment.service

import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentResult
import com.coco.payment.service.dto.PrepareBillingPaymentResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentWorkflowService(
    private val orderService: OrderService,
    private val billingPaymentService: BillingPaymentService,
) {
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
        return billingPaymentService.prepare(order.id!!, command)
    }

    @Transactional
    fun complete(prepared: PrepareBillingPaymentResult, result: TossBillingPaymentResult) {
        billingPaymentService.complete(prepared, result)
        orderService.markPaid(prepared.orderId)
    }

    @Transactional
    fun fail(prepared: PrepareBillingPaymentResult) {
        billingPaymentService.fail(prepared)
        orderService.markPaymentFailed(prepared.orderId)
    }
}
