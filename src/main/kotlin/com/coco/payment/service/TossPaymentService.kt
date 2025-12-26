package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.PgError
import com.coco.payment.handler.paymentgateway.TossApiException
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

        return BillingView.ConfirmResult.TossConfirmResult(
            PaymentSystem.TOSS,
            response.paymentKey,
            response.type,
            response.mId,
            response.lastTransactionKey,
            response.orderId,
            response.totalAmount,
            response.balanceAmount,
            response.status,
            response.requestedAt,
            response.approvedAt,
            response.taxFreeAmount
        )
    }

    // todo 흠 맘에 안들지만, 이게 중요한 목적이 아니니.
    fun findTransaction(
        externalOrderKey: String
    ): PgResult<BillingView.TransactionResult.TossTransactionResult> {
        return try {
            val body = tossPaymentClient.findTransaction(externalOrderKey)

            if (body.isDone()) {
                PgResult.Success(
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
            } else if (body.isFail()) {
                PgResult.Fail(
                    PgError("PgErrorCode.BUSINESS", "결제 실패 상태")
                )
            } else {
                PgResult.Retryable(
                    PgError("PgErrorCode.UNKNOWN", "확정되지 않은 결제 상태")
                )
            }

        } catch (e: TossApiException) {
            tossErrorResolver.findTransactionErrorResolver(
                e.status,
                e.code
            )
        }
    }
}
