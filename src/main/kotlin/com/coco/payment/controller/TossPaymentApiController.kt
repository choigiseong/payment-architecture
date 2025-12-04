package com.coco.payment.controller

import com.coco.payment.controller.dto.TossBillingView
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.PaymentService
import com.coco.payment.service.TossPaymentService
import com.coco.payment.service.dto.BillingKeyDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TossPaymentApiController(
    private val paymentService: PaymentService,
    private val tossPaymentService: TossPaymentService
) {

    private val paymentSystem = PaymentSystem.TOSS

    @RequestMapping(value = ["/issue-billing-key"])
    fun issueBillingKey(@RequestBody request: TossBillingView.BillingKeyRequest): ResponseEntity<BillingKeyDto> {
        val billingKeyDto = tossPaymentService.issueBillingKey(
            request.customerKey,
            request.authKey
        )
        paymentService.registerBillingKey(
            request.customerKey,
            billingKeyDto
        )
        return ResponseEntity.ok(billingKeyDto)
    }

    @RequestMapping(value = ["/confirm-billing"])
    fun confirmBilling(@RequestBody request: TossBillingView.ConfirmBillingRequest): ResponseEntity<TossBillingView.ConfirmBillingResponse> {
        val billingKeyModel =
            paymentService.findBillingKey(request.customerKey, paymentSystem)
                ?: return ResponseEntity.badRequest().body(null)
        tossPaymentService.confirmBilling(
            request.customerKey,
            billingKeyModel.billingKey,
            request.amount,
            request.customerEmail,
            request.customerName,
            request.orderId,
            request.orderName
        )
        return ResponseEntity.ok(TossBillingView.ConfirmBillingResponse(request.orderId))
    }
}