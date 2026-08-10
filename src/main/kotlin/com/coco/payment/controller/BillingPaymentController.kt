package com.coco.payment.controller

import com.coco.payment.controller.dto.BillingPaymentRequest
import com.coco.payment.controller.dto.BillingPaymentResponse
import com.coco.payment.controller.dto.PaymentPollingResponse
import com.coco.payment.service.facade.BillingPaymentFacade
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/payments")
class BillingPaymentController(private val billingPaymentFacade: BillingPaymentFacade) {
    @PostMapping
    fun pay(@Valid @RequestBody request: BillingPaymentRequest): BillingPaymentResponse =
        BillingPaymentResponse.of(billingPaymentFacade.pay(request.toCommand()))

    @GetMapping("/{paymentKey}")
    fun poll(@PathVariable paymentKey: String): ResponseEntity<PaymentPollingResponse> =
        billingPaymentFacade.poll(paymentKey)
            ?.let { ResponseEntity.ok(PaymentPollingResponse.of(it)) }
            ?: ResponseEntity.notFound().build()
}
