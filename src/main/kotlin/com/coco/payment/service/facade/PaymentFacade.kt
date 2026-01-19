package com.coco.payment.service.facade

import com.coco.payment.handler.paymentgateway.dto.PgResult
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.CustomerPaymentBillingKey
import com.coco.payment.service.CustomerService
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.PaymentAttemptService
import com.coco.payment.service.RefundAttemptService
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.dto.PrepaymentView
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
        paymentSystem: PaymentSystem,
        externalOrderKey: String,
    ): PgResult<BillingView.TransactionResult> {
        val strategy = strategyManager.billingPaymentResolve(paymentSystem)
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
            requestedAt
        )

        val strategy = strategyManager.billingPaymentResolve(confirmBillingCommand.paymentSystem)
        return strategy.confirmBilling(
            confirmBillingCommand
        )
    }

    fun refundBilling(
        invoiceSeq: Long,
        at: Instant,
        command: BillingView.RefundBillingCommand
    ): BillingView.RefundResult {
        refundAttemptService.createAttemptIfRefundable(
            invoiceSeq = invoiceSeq,
            requestAmount = command.amount,
            reason = command.reason,
            at = at,
            refundItems = emptyList()
        )

        val strategy = strategyManager.billingPaymentResolve(command.paymentSystem)
        return strategy.refundBilling(command)
    }


    fun successBilling(
        confirmResult: BillingView.ConfirmResult
    ) {
        val strategy = strategyManager.billingPaymentResolve(confirmResult.paymentSystem)
        strategy.onSuccessBilling(confirmResult)
    }

    fun successRefundBilling(
        refundResult: BillingView.RefundResult
    ) {
        val strategy = strategyManager.billingPaymentResolve(refundResult.paymentSystem)
        strategy.onSuccessRefundBilling(refundResult)
    }


    @Transactional
    fun failPayment(invoiceSeq: Long, at: Instant, failedReason: String) {
        paymentAttemptService.failed(invoiceSeq, at, failedReason)
        invoiceService.handleRetryOrFinalFailed(invoiceSeq, at)
    }

    fun confirmPrepayment(
        invoiceSeq: Long,
        command: PrepaymentView.ConfirmPrepaymentCommand,
        at: Instant
    ): PrepaymentView.ConfirmResult {
        paymentAttemptService.createPaymentAttempt(
            invoiceSeq,
            at
        )

        val strategy = strategyManager.prepaymentPaymentResolve(command.paymentSystem)
        return strategy.confirmPrepayment(command)
    }

    fun successPrepayment(confirmResult: PrepaymentView.ConfirmResult) {
        val strategy = strategyManager.prepaymentPaymentResolve(confirmResult.paymentSystem)
        strategy.onSuccessPrepayment(confirmResult)
    }

    fun refundPrepayment(
        invoiceSeq: Long,
        at: Instant,
        command: PrepaymentView.RefundPrepaymentCommand,
        claimSeq: Long? = null
    ): PrepaymentView.RefundResult {
        refundAttemptService.createAttemptIfRefundable(
            invoiceSeq = invoiceSeq,
            requestAmount = command.amount,
            reason = command.reason,
            at = at,
            refundItems = command.refundItems,
            claimSeq = claimSeq
        )

        val strategy = strategyManager.prepaymentPaymentResolve(command.paymentSystem)
        return strategy.refundPrepayment(command)
    }

    fun successRefundPrepayment(refundResult: PrepaymentView.RefundResult, refundAmount: Long) {
        val strategy = strategyManager.prepaymentPaymentResolve(refundResult.paymentSystem)
        strategy.onSuccessRefundPrepayment(refundResult, refundAmount)
    }

}