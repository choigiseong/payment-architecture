package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.repository.CompanyBillingKeyRepository
import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.service.dto.PrepareBillingPaymentResult
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BillingPaymentService(
    private val paymentTransactionService: PaymentTransactionService,
    private val companyBillingKeyRepository: CompanyBillingKeyRepository,
) {
    fun findByPaymentKey(paymentKey: String) = paymentTransactionService.findByPaymentKey(paymentKey)

    @Transactional
    fun prepare(orderId: Long, command: BillingPaymentCommand): PrepareBillingPaymentResult {
        val moid = UUID.randomUUID().toString()
        val paymentTransactionId = paymentTransactionService.createPending(command.paymentKey, orderId, moid, command.totalPrice)
        val billingKey = companyBillingKeyRepository.findByCompanySeqAndPaymentSystem(command.companySeq, PaymentSystem.TOSS)
            ?: throw IllegalArgumentException("Toss billing key not found for company: ${command.companySeq}")
        return PrepareBillingPaymentResult(orderId, paymentTransactionId, command.orderKey, command.paymentKey, billingKey.billingKey, billingKey.customerKey, moid, command.orderName, command.totalPrice)
    }

    @Transactional
    fun complete(prepared: PrepareBillingPaymentResult, result: TossBillingPaymentResult) {
        paymentTransactionService.complete(prepared.paymentTransactionId, result.tid)
    }

    @Transactional
    fun fail(prepared: PrepareBillingPaymentResult) {
        paymentTransactionService.fail(prepared.paymentTransactionId)
    }
}
