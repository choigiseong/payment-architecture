package com.coco.payment.controller

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.PaymentService
import com.coco.payment.view.TossPaymentView
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TossPaymentApiController(
    private val paymentService: PaymentService,
) {
    private val PAYMENT_SYSTEM = PaymentSystem.TOSS

    @RequestMapping(value = ["/issue-billing-key"])
    fun issueBillingKey(@RequestBody request: TossPaymentView.BillingKeyRequest): ResponseEntity<Void> {
        paymentService.registerBillingKey(
            PAYMENT_SYSTEM,
            request.customerKey,
            request.authKey
        )
        return ResponseEntity.ok().build()
    }
}