package com.coco.payment

import com.coco.payment.view.TossPaymentView
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody


@FeignClient(
    name = "tossPayment",
    url = "\${payment.toss.api.base-url}",
)
interface TossPaymentClient {
//    @PostMapping("\${payment.toss.api.endpoint.payment-confirm}")
//    fun confirmPayment(
//        @RequestBody request: TossConfirmRequest
//    ): TossConfirmResponse

    @PostMapping("\${payment.toss.api.endpoint.billing-key-issue}")
    fun issueBillingKey(
        @RequestBody request: TossPaymentView.BillingKeyRequest
    ): TossPaymentView.BillingKeyResponse
}