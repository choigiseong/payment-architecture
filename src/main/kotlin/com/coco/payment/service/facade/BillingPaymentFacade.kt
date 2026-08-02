package com.coco.payment.service.facade

import com.coco.payment.handler.paymentgateway.toss.TossBillingPaymentCommand
import com.coco.payment.handler.paymentgateway.toss.TossBillingPaymentHandler
import com.coco.payment.handler.paymentgateway.toss.TossBillingPaymentResult
import com.coco.payment.service.BillingPaymentCommand
import com.coco.payment.service.BillingPaymentService
import org.springframework.stereotype.Service

@Service
class BillingPaymentFacade(
    private val billingPaymentService: BillingPaymentService,
    private val tossBillingPaymentHandler: TossBillingPaymentHandler,
) {
    fun pay(command: BillingPaymentCommand): TossBillingPaymentResult {
        val prepared = billingPaymentService.prepare(command)
        val result = tossBillingPaymentHandler.approve(
            TossBillingPaymentCommand(prepared.billingKey, prepared.customerKey, prepared.moid, prepared.orderName, prepared.amount)
        )
        billingPaymentService.complete(prepared, result)
        return result
    }
}
