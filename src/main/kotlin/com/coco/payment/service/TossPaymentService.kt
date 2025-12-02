package com.coco.payment.service

import com.coco.payment.handler.TossPaymentClient
import com.coco.payment.service.dto.BillingKeyDto
import com.coco.payment.view.TossPaymentView
import org.springframework.stereotype.Service

@Service
class TossPaymentService(
    private val tossPaymentClient: TossPaymentClient
) {

    fun issueBillingKey(
        customerKey: String,
        authKey: String
    ): BillingKeyDto {
        val response = tossPaymentClient.issueBillingKey(
            TossPaymentView.BillingKeyRequest(
                customerKey,
                authKey
            )
        ).getOrThrow()
        // todo 뭔가

        return BillingKeyDto(
            response.billingKey,
            response.cardNumber,
            response.cardCompany
        )
    }

}