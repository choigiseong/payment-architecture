package com.coco.payment.handler.paymentgateway.toss

import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentCommand
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentResult
import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class TossBillingPaymentHandler(
    private val tossRestClient: RestClient,
    @Value("\${payment.toss.secret-key}")
    private val secretKey: String,
) {
    fun approve(command: TossBillingPaymentCommand): PaymentResult<TossBillingPaymentResult> {
        require(secretKey.isNotBlank()) { "TOSS_SECRET_KEY must be configured" }

        return try {
            val response = tossRestClient.post()
                .uri("/v1/billing/{billingKey}", command.billingKey)
                .headers { headers -> headers.setBasicAuth(secretKey, "") }
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    TossBillingPaymentRequest(
                        customerKey = command.customerKey,
                        orderId = command.moid,
                        orderName = command.orderName,
                        amount = command.amount,
                    )
                )
                .retrieve()
                .onStatus({ status -> status.isError }) { _, clientResponse ->
                    throw TossPaymentException(
                        code = clientResponse.statusCode.value().toString(),
                        message = "Toss billing payment failed: ${clientResponse.statusCode}",
                    )
                }
                .body(TossBillingPaymentResponse::class.java)
                ?: return PaymentResult.Unknown(PaymentResult.PaymentError(null, "Toss billing payment response is empty"))

            PaymentResult.Success(
                TossBillingPaymentResult(
                    tid = response.paymentKey,
                    moid = response.orderId,
                    amount = response.totalAmount,
                )
            )
        } catch (exception: TossPaymentException) {
            PaymentResult.Failure(PaymentResult.PaymentError(exception.code, exception.message ?: "Toss billing payment failed"))
        } catch (exception: RestClientException) {
            PaymentResult.Unknown(PaymentResult.PaymentError(null, exception.message ?: "Toss billing payment request failed"))
        }
    }
}

private data class TossBillingPaymentRequest(
    val customerKey: String,
    val orderId: String,
    val orderName: String,
    val amount: Long,
)

private data class TossBillingPaymentResponse(
    val paymentKey: String,
    val orderId: String,
    val totalAmount: Long,
    val status: String,
)

private class TossPaymentException(
    val code: String?,
    message: String,
) : RuntimeException(message)
