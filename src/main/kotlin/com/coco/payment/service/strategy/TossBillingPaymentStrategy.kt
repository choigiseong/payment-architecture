package com.coco.payment.service.strategy

import com.coco.payment.handler.paymentgateway.dto.PgResult
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.TossPaymentService
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.LedgerService
import com.coco.payment.service.PaymentAttemptService
import com.coco.payment.service.SubscriptionService
import com.coco.payment.service.TossPaymentEventService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TossBillingPaymentStrategy(
    private val tossPaymentService: TossPaymentService,
    private val tossPaymentEventService: TossPaymentEventService,
    private val invoiceService: InvoiceService,
    private val paymentAttemptService: PaymentAttemptService,
    private val subscriptionService: SubscriptionService,
    private val ledgerService: LedgerService
) : BillingPaymentStrategy {
    override fun supports(): PaymentSystem = PaymentSystem.TOSS

    override fun confirmBilling(
        billingKey: String,
        command: BillingView.ConfirmBillingCommand
    ): BillingView.ConfirmResult.TossConfirmResult {
        return tossPaymentService.confirmBilling(billingKey, command)
    }

    @Transactional
    override fun onSuccess(
        customerSeq: Long,
        confirmResult: BillingView.ConfirmResult
    ) {
        if (confirmResult !is BillingView.ConfirmResult.TossConfirmResult) {
            throw IllegalArgumentException("Provider response is not TossPaymentConfirmResponse")
        }
        val invoice = invoiceService.findByExternalKey(
            confirmResult.orderId,
        )
        subscriptionService.renew(
            invoice.subscriptionSeq,
            invoice.periodEnd
        )
        invoiceService.paid(
            invoice.id!!,
            confirmResult.approvedAt,
        )
        paymentAttemptService.succeeded(
            invoice.id!!,
            confirmResult.approvedAt,
            confirmResult.paymentKey,
        )
        val ledger = ledgerService.createLedger(
            customerSeq,
        )
        tossPaymentEventService.createTossPaymentEvent(
            customerSeq,
            ledger.id!!,
            confirmResult
        )
    }

    override fun findTransaction(externalOrderKey: String): PgResult<BillingView.TransactionResult.TossTransactionResult> {
        return tossPaymentService.findTransaction(externalOrderKey)
    }
}

