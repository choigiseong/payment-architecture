package com.coco.payment.service.strategy

import com.coco.payment.handler.paymentgateway.dto.PgResult
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.TossPaymentService
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.LedgerService
import com.coco.payment.service.PaymentAttemptService
import com.coco.payment.service.RefundAttemptService
import com.coco.payment.service.SubscriptionService
import com.coco.payment.service.TossPaymentEventService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TossBillingPaymentStrategy(
    private val ledgerService: LedgerService,
    private val invoiceService: InvoiceService,
    private val tossPaymentService: TossPaymentService,
    private val subscriptionService: SubscriptionService,
    private val refundAttemptService: RefundAttemptService,
    private val paymentAttemptService: PaymentAttemptService,
    private val tossPaymentEventService: TossPaymentEventService,
) : BillingPaymentStrategy {
    override fun supports(paymentSystem: PaymentSystem): Boolean = paymentSystem == PaymentSystem.TOSS


    override fun findTransaction(externalOrderKey: String): PgResult<BillingView.TransactionResult.TossTransactionResult> {
        return tossPaymentService.findTransaction(externalOrderKey)
    }

    override fun confirmBilling(
        command: BillingView.ConfirmBillingCommand
    ): BillingView.ConfirmResult.TossConfirmResult {
        return tossPaymentService.confirmBilling(command)
    }

    @Transactional
    override fun onSuccessBilling(
        confirmResult: BillingView.ConfirmResult
    ) {
        if (confirmResult !is BillingView.ConfirmResult.TossConfirmResult) {
            throw IllegalArgumentException("Provider response is not TossPaymentConfirmResponse")
        }
        val invoice = invoiceService.findByExternalOrderKey(
            confirmResult.orderId,
        )
        val subscription = subscriptionService.findById(invoice.subscriptionSeq!!)
        subscriptionService.renew(
            invoice.subscriptionSeq!!,
            invoice.periodEnd!!
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
            subscription.customerSeq,
        )
        tossPaymentEventService.createTossPaymentEvent(
            subscription.customerSeq,
            ledger.id!!,
            confirmResult.paymentKey,
            "Approve"
        )
    }

    override fun refundBilling(
        command: BillingView.RefundBillingCommand
    ): BillingView.RefundResult.TossRefundResult {
        return tossPaymentService.cancelBilling(command)
    }

    @Transactional
    override fun onSuccessRefundBilling(refundResult: BillingView.RefundResult) {
        if (refundResult !is BillingView.RefundResult.TossRefundResult) {
            throw IllegalArgumentException("Provider response is not TossCancelResult")
        }
        val invoice = invoiceService.findByExternalOrderKey(refundResult.orderId)
        val subscription = subscriptionService.findById(invoice.subscriptionSeq!!)

        if (!refundResult.isRefundable()) {
            subscriptionService.cancel(subscription.id!!)
            invoiceService.refunded(invoice.id!!)
        } else {
            invoiceService.partiallyRefunded(invoice.id!!)
        }

        // todo 동시 다중 환불 요청을 방지하기 위해, 환불 처리 중인 경우 다른 환불 요청을 차단해야 합니다.
        // 이게 최선인가?
        val lastCanceled = refundResult.getLastCanceledInfo()
        refundAttemptService.succeeded(
            invoice.id!!,
            lastCanceled.canceledAt,
            lastCanceled.transactionKey
        )

        // 원장/이벤트 기록 (선택, 메서드 추가 필요)
//         val ledger = ledgerService.createLedger(subscription.customerSeq)
        // todo ledger는 비동기로?
        tossPaymentEventService.createTossPaymentEvent(
            subscription.customerSeq,
            1,
            lastCanceled.transactionKey,
            "Cancel"
        )
    }
}
