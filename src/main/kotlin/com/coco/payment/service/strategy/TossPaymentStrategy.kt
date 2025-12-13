package com.coco.payment.service.strategy

import com.coco.payment.handler.dto.TossPaymentView
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.TossPaymentService
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.LedgerService
import com.coco.payment.service.TossPaymentEventService
import com.coco.payment.handler.dto.ConfirmBillingResponse
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
    ): ConfirmBillingResponse {
        return tossPaymentService.confirmBilling(billingKey, command)
    }

    @Transactional
    override fun onSuccess(
        customerId: Long,
        providerResponse: ConfirmBillingResponse
    ) {
        if (providerResponse !is TossPaymentView.TossConfirmBillingBillingResponse) {
            throw IllegalArgumentException("Provider response is not TossPaymentConfirmResponse")
        }
        tossPaymentEventService.createTossPaymentEvent(customerId, providerResponse)
        ledgerService.createLedger()
    }
}
