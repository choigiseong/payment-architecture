package com.coco.payment.service

import com.coco.payment.handler.TossPaymentClient
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.dto.BillingKeyDto
import com.coco.payment.handler.dto.TossPaymentView
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
            TossPaymentView.TossBillingKeyRequest(
                customerKey,
                authKey
            )
        )
        // todo 뭔가 result로

        return BillingKeyDto(
            PaymentSystem.TOSS,
            response.billingKey,
            response.cardNumber,
            response.cardCompany
        )
    }

    fun confirmBilling(
        customerKey: String,
        billingKey: String,
        amount: Long,
        customerEmail: String,
        customerName: String,
        orderId: String,
        orderName: String,
    ): String {
        val response = tossPaymentClient.confirmBilling(
            billingKey,
            TossPaymentView.TossConfirmBillingRequest(
                customerKey,
                amount,
                customerEmail,
                customerName,
                orderId,
                orderName
            )
        )

        return response.orderId
    }

}