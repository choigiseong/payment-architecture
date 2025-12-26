package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.PgError
import com.coco.payment.handler.paymentgateway.TossErrorResolver
import com.coco.payment.handler.paymentgateway.TossPaymentClient
import com.coco.payment.handler.paymentgateway.dto.PgResult
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.handler.paymentgateway.dto.TossPaymentView
import com.coco.payment.service.dto.BillingView
import org.springframework.stereotype.Service

@Service
class TossPaymentService(
    private val tossPaymentClient: TossPaymentClient
) {

    private val tossErrorResolver = TossErrorResolver()

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
        if (response.statusCode.isError) {
            throw IllegalArgumentException("TossPaymentService.issueBillingKey")
        }

        val body = response.body ?: throw IllegalArgumentException("TossPaymentService.issueBillingKey")
        return BillingView.BillingKeyResult(
            PaymentSystem.TOSS,
            body.billingKey,
            body.cardNumber,
            body.cardCompany
        )
    }

    fun confirmBilling(
        billingKey: String,
        confirmBillingCommand: BillingView.ConfirmBillingCommand
    ): BillingView.ConfirmResult.TossConfirmResult {
        val response = tossPaymentClient.confirmBilling(
            billingKey,
            TossPaymentView.TossConfirmBillingRequest(
                confirmBillingCommand.customerSeq.toString(),
                confirmBillingCommand.amount,
                confirmBillingCommand.customerEmail,
                confirmBillingCommand.customerName,
                confirmBillingCommand.orderId,
                confirmBillingCommand.orderName
            )
        )

        if (response.statusCode.isError) {
            throw IllegalArgumentException("TossPaymentService.confirmBilling")
        }

        val body = response.body ?: throw IllegalArgumentException("TossPaymentService.confirmBilling")
        return BillingView.ConfirmResult.TossConfirmResult(
            PaymentSystem.TOSS,
            body.paymentKey,
            body.type,
            body.mId,
            body.lastTransactionKey,
            body.orderId,
            body.totalAmount,
            body.balanceAmount,
            body.status,
            body.requestedAt,
            body.approvedAt,
            body.taxFreeAmount
        )
    }

    //todo 확인
    fun findTransaction(
        externalOrderKey: String
    ): PgResult<BillingView.TransactionResult.TossTransactionResult> {

        val response = tossPaymentClient.findTransaction(externalOrderKey)

        if (response.statusCode.isError) {
            return tossErrorResolver.resolve(
                response.statusCode,
                response.body?.code
            )
        }

        val body = response.body
            ?: return PgResult.Retryable(
                PgError(
                    PgErrorCode.UNKNOWN,
                    "응답 바디 없음",
                    null
                )
            )

        return PgResult.Success(
            BillingView.TransactionResult.TossTransactionResult(
                PaymentSystem.TOSS,
                body.paymentKey,
                body.type,
                body.mId,
                body.lastTransactionKey,
                body.orderId,
                body.totalAmount,
                body.balanceAmount,
                body.status,
                body.requestedAt,
                body.approvedAt,
                body.taxFreeAmount,
            )
        )
    }


}