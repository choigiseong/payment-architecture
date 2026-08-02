package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.toss.TossBillingPaymentResult
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.repository.CompanyBillingKeyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BillingPaymentService(
    private val orderService: OrderService,
    private val paymentTransactionService: PaymentTransactionService,
    private val companyBillingKeyRepository: CompanyBillingKeyRepository,
) {
    @Transactional
    fun prepare(command: BillingPaymentCommand): PreparedBillingPayment {
        val orderId = orderService.createPendingOrder(command.companySeq, command.totalPrice, command.items)
        val paymentTransactionId = paymentTransactionService.createPending(orderId, command.moid, command.totalPrice)
        val billingKey = companyBillingKeyRepository.findByCompanySeqAndPaymentSystem(command.companySeq, PaymentSystem.TOSS)
            ?: throw IllegalArgumentException("Toss billing key not found for company: ${command.companySeq}")
        return PreparedBillingPayment(orderId, paymentTransactionId, billingKey.billingKey, billingKey.customerKey, command.moid, command.orderName, command.totalPrice)
    }

    @Transactional
    fun complete(prepared: PreparedBillingPayment, result: TossBillingPaymentResult) {
        paymentTransactionService.complete(prepared.paymentTransactionId, result.tid)
        orderService.markPaid(prepared.orderId)
    }
}

data class BillingPaymentCommand(val companySeq: Long, val moid: String, val orderName: String, val totalPrice: Long, val items: List<BillingOrderItem>)
data class PreparedBillingPayment(val orderId: Long, val paymentTransactionId: Long, val billingKey: String, val customerKey: String, val moid: String, val orderName: String, val amount: Long)
