package com.coco.payment.handler.paymentgateway

import com.coco.payment.handler.paymentgateway.dto.TossPaymentView
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody


@FeignClient(
    name = "tossPayment",
    url = "\${payment.toss.api.base-url}",
)
interface TossPaymentClient {

    @PostMapping("\${payment.toss.api.endpoint.billing-key-issue}")
    fun issueBillingKey(
        @RequestBody request: TossPaymentView.TossBillingKeyRequest
    ): ResponseEntity<TossPaymentView.TossBillingKeyResponse>

    @PostMapping("\${payment.toss.api.endpoint.confirm-billing}/{billingKey}")
    fun confirmBilling(
        @PathVariable billingKey: String,
        @RequestBody request: TossPaymentView.TossConfirmBillingRequest
    ): ResponseEntity<TossPaymentView.TossConfirmBillingBillingResponse>

    @GetMapping("\${payment.toss.api.endpoint.transaction}/{externalOrderKey}")
    fun findTransaction(
        @PathVariable externalOrderKey: String
    ): ResponseEntity<Any>
}