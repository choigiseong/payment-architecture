package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.repository.CompanyBillingKeyRepository
import com.coco.payment.service.dto.BillingOrderItem
import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.service.dto.BillingPaymentResult
import com.coco.payment.service.dto.PrepareBillingPaymentResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BillingPaymentService(
    private val orderService: OrderService,
    private val paymentTransactionService: PaymentTransactionService,
    private val companyBillingKeyRepository: CompanyBillingKeyRepository,
) {
    @Transactional
    fun prepare(command: BillingPaymentCommand): PrepareBillingPaymentResult {
        val orderId = orderService.createPendingOrder(command.companySeq, command.totalPrice, command.items)
        val paymentTransactionId = paymentTransactionService.createPending(orderId, command.moid, command.totalPrice)
        val billingKey = companyBillingKeyRepository.findByCompanySeqAndPaymentSystem(command.companySeq, PaymentSystem.TOSS)
            ?: throw IllegalArgumentException("Toss billing key not found for company: ${command.companySeq}")
        return PrepareBillingPaymentResult(orderId, paymentTransactionId, billingKey.billingKey, billingKey.customerKey, command.moid, command.orderName, command.totalPrice)
    }

    @Transactional
    fun complete(prepared: PrepareBillingPaymentResult, result: BillingPaymentResult) {
        paymentTransactionService.complete(prepared.paymentTransactionId, result.tid)
        orderService.markPaid(prepared.orderId)
    }

    @Transactional
    fun fail(prepared: PrepareBillingPaymentResult) {
        paymentTransactionService.fail(prepared.paymentTransactionId)
        orderService.markPaymentFailed(prepared.orderId)
    }
}
