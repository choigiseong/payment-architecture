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

    // todo 성공, 실패, 알 수 없음.

    fun issueBillingKey(
        customerKey: String,
        authKey: String
    ): BillingView.BillingKeyResult {
        val response = tossPaymentClient.issueBillingKey(
            TossPaymentView.TossBillingKeyRequest(
                customerKey,
                authKey
            )
        )
        // todo result로

        return BillingView.BillingKeyResult(
            PaymentSystem.TOSS,
            response.billingKey,
            response.cardNumber,
            response.cardCompany
        )
    }

    fun confirmBilling(
        billingKey: String,
        confirmBillingCommand: BillingView.ConfirmBillingCommand
    ): BillingView.ConfirmBillingResult {
        val responseResult = runCatching {
            tossPaymentClient.confirmBilling(
                billingKey,
                TossPaymentView.TossConfirmBillingRequest(
                    confirmBillingCommand.customerKey,
                    confirmBillingCommand.amount,
                    confirmBillingCommand.customerEmail,
                    confirmBillingCommand.customerName,
                    confirmBillingCommand.orderId,
                    confirmBillingCommand.orderName
                )
            )
        }

        // 통일된 dto로 반환
        responseResult
            .onSuccess {
                return BillingView.ConfirmBillingResult(
                    it.orderId,
                    it.paymentKey,
                    PaymentSystem.TOSS,
                    it.status,
                    it.requestedAt,
                    it.approvedAt,
                    it.taxFreeAmount
                )
            }
            .onFailure {
                throw it
            }
        throw IllegalArgumentException("TossPaymentService.confirmBilling")
    }

}