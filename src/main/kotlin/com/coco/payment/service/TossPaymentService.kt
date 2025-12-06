package com.coco.payment.service

import com.coco.payment.handler.TossPaymentClient
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.handler.dto.TossPaymentView
import com.coco.payment.service.dto.BillingView
import org.springframework.stereotype.Service

@Service
class TossPaymentService(
    private val tossPaymentClient: TossPaymentClient
) {

    fun issueBillingKey(
        customerKey: String,
        authKey: String
    ): BillingView.BillingKeyDto {
        val response = tossPaymentClient.issueBillingKey(
            TossPaymentView.TossBillingKeyRequest(
                customerKey,
                authKey
            )
        )
        // todo 뭔가 result로

        return BillingView.BillingKeyDto(
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
        val responseResult = runCatching {
            tossPaymentClient.confirmBilling(
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
        }

        responseResult
            .onSuccess {
                return it.orderId
            }
            .onFailure {
                throw it
            }
        return ""
    }

}