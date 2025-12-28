package com.coco.payment.service.facade

import com.coco.payment.handler.paymentgateway.dto.PgResult
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.CustomerPaymentBillingKey
import com.coco.payment.service.CustomerService
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.PaymentAttemptService
import com.coco.payment.service.RefundAttemptService
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.strategy.PaymentStrategyManager
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PaymentFacade(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val strategyManager: PaymentStrategyManager,
    private val paymentAttemptService: PaymentAttemptService,
    private val refundAttemptService: RefundAttemptService,
) {

    fun findBillingKey(customerSeq: Long, paymentSystem: PaymentSystem): CustomerPaymentBillingKey? {
        val customer = customerService.findById(customerSeq)
        return customer.billingKeys.find { it.paymentSystem == paymentSystem }
    }

    fun findTransaction(
        externalOrderKey: String,
        paymentSystem: PaymentSystem
    ): PgResult<BillingView.TransactionResult> {
        val strategy = strategyManager.resolve(paymentSystem)
        return strategy.findTransaction(externalOrderKey)
    }

    fun registerBillingKey(
        customerKey: String, billingKeyResult: BillingView.BillingKeyResult
    ) {
        val customer = customerService.findByCustomerKey(customerKey)
        customerService.addBillingKey(
            customer.id!!,
            billingKeyResult,
        )
    }

    fun confirmBilling(
        invoiceSeq: Long,
        requestedAt: Instant,
        confirmBillingCommand: BillingView.ConfirmBillingCommand
    ): BillingView.ConfirmResult {
        paymentAttemptService.createPaymentAttempt(
            invoiceSeq,
            confirmBillingCommand.paymentSystem,
            requestedAt
        )

        val strategy = strategyManager.resolve(confirmBillingCommand.paymentSystem)
        return strategy.confirmBilling(
            confirmBillingCommand
        )
    }

    fun refund(
        invoiceSeq: Long,
        paymentAttemptSeq: Long,
        at: Instant,
        command: BillingView.RefundBillingCommand
    ): BillingView.RefundResult {
        refundAttemptService.createAttempt(
            invoiceSeq = invoiceSeq,
            paymentAttemptSeq = paymentAttemptSeq,
            amount = command.amount,
            reason = command.reason,
            at = at
        )

        val strategy = strategyManager.resolve(command.paymentSystem)
        return strategy.refundBilling(
            command
        )
    }


    fun successBilling(
        confirmResult: BillingView.ConfirmResult
    ) {
        val strategy = strategyManager.resolve(confirmResult.paymentSystem)
        strategy.onSuccessBilling(confirmResult)
    }

    fun successRefundBilling(
        refundResult: BillingView.RefundResult
    ) {
        val strategy = strategyManager.resolve(refundResult.paymentSystem)
        strategy.onSuccessRefundBilling(refundResult)
    }


    @Transactional
    fun failPayment(invoiceSeq: Long, at: Instant, failedReason: String) {
        paymentAttemptService.failed(invoiceSeq, at, failedReason)
        invoiceService.handleRetryOrFinalFailed(invoiceSeq, at)
    }

}