package com.coco.payment.handler

import com.coco.payment.handler.dto.TossPaymentView
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody


@FeignClient(
    name = "tossPayment",
    url = "\${payment.toss.api.base-url}",
)
interface TossPaymentClient {

    // todo result
    @PostMapping("\${payment.toss.api.endpoint.billing-key-issue}")
    fun issueBillingKey(
        @RequestBody request: TossPaymentView.TossBillingKeyRequest
    ): TossPaymentView.TossBillingKeyResponse

    @PostMapping("\${payment.toss.api.endpoint.confirm-billing}/{billingKey}")
    fun confirmBilling(
        @PathVariable billingKey: String,
        @RequestBody request: TossPaymentView.TossConfirmBillingRequest
    ): TossPaymentView.TossConfirmBillingResponse
}