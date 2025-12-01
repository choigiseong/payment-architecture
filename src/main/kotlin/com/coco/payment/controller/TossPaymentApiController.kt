package com.coco.payment.controller

import com.coco.payment.TossPaymentClient
import com.coco.payment.service.CustomerService
import com.coco.payment.view.TossPaymentView
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TossPaymentApiController(
    private val customerService: CustomerService,
    private val tossPaymentClient: TossPaymentClient,
    @Value("\${payment.toss.api-key}")
    private val tossPayApiKey: String
) {

    @RequestMapping(value = ["/issue-billing-key"])
    fun issueBillingKey(@RequestBody request: TossPaymentView.BillingKeyRequest): ResponseEntity<TossPaymentView.BillingKeyResponse> {
        val response = tossPaymentClient.issueBillingKey(request)
        return ResponseEntity.ok(response)
    }
}