package com.coco.payment.service.facade

import com.coco.payment.handler.paymentgateway.toss.TossBillingPaymentHandler
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentCommand
import com.coco.payment.service.BillingPaymentService
import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.service.dto.BillingPaymentResult
import org.springframework.stereotype.Service

@Service
class BillingPaymentFacade(
    private val billingPaymentService: BillingPaymentService,
    // 추후 pg 확장 가능하게 분리
    private val tossBillingPaymentHandler: TossBillingPaymentHandler,
) {
    fun pay(command: BillingPaymentCommand): BillingPaymentResult {
        val prepared = billingPaymentService.prepare(command)
        val result = tossBillingPaymentHandler.approve(
            TossBillingPaymentCommand(prepared.billingKey, prepared.customerKey, prepared.moid, prepared.orderName, prepared.amount)
        )
        billingPaymentService.complete(prepared, result)
        return result
    }
}
