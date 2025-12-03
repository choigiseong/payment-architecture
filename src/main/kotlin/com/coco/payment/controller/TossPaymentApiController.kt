package com.coco.payment.controller

import com.coco.payment.controller.dto.BillingKeyRequest
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

    @RequestMapping(value = ["/issue-billing-key"])
    fun issueBillingKey(@RequestBody request: BillingKeyRequest): ResponseEntity<BillingKeyDto> {
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
}