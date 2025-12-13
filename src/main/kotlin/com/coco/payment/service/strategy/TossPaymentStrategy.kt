package com.coco.payment.service.strategy

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.TossPaymentService
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.LedgerService
import com.coco.payment.service.TossPaymentEventService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class TossPaymentStrategy(
    private val tossPaymentService: TossPaymentService,
    private val tossPaymentEventService: TossPaymentEventService,
    private val ledgerService: LedgerService
) : PaymentStrategy {
    override fun supports(): PaymentSystem = PaymentSystem.TOSS

    override fun confirmBilling(
        billingKey: String,
        command: BillingView.ConfirmBillingCommand
    ): BillingView.ConfirmResult.TossConfirmResult {
        return tossPaymentService.confirmBilling(billingKey, command)
    }

    @Transactional
    override fun onSuccess(
        customerId: Long,
        confirmResult: BillingView.ConfirmResult
    ) {
        if (confirmResult !is BillingView.ConfirmResult.TossConfirmResult) {
            throw IllegalArgumentException("Provider response is not TossPaymentConfirmResponse")
        }
        val ledger = ledgerService.createLedger(
            customerId,
        )
        tossPaymentEventService.createTossPaymentEvent(
            customerId,
            ledger.id!!,
            confirmResult
        )

    }
}
