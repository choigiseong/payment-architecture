package com.coco.payment.handler.paymentgateway.toss

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TossBillingPaymentHandler(
    private val tossRestClient: RestClient,
    @Value("\${payment.toss.secret-key}")
    private val secretKey: String,
) {
    fun approve(command: TossBillingPaymentCommand): TossBillingPaymentResult {
        require(secretKey.isNotBlank()) { "TOSS_SECRET_KEY must be configured" }

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
                val error = clientResponse.body(TossErrorResponse::class.java)
                throw TossPaymentException(
                    status = clientResponse.statusCode,
                    code = error?.code,
                    message = error?.message ?: "Toss billing payment failed",
                )
            }
            .body(TossBillingPaymentResponse::class.java)
            ?: throw TossPaymentException(
                status = null,
                code = null,
                message = "Toss billing payment response is empty",
            )

        return TossBillingPaymentResult(
            tid = response.paymentKey,
            moid = response.orderId,
            amount = response.totalAmount,
            status = response.status,
        )
    }
}

data class TossBillingPaymentCommand(
    val billingKey: String,
    val customerKey: String,
    val moid: String,
    val orderName: String,
    val amount: Long,
)

data class TossBillingPaymentResult(
    val tid: String,
    val moid: String,
    val amount: Long,
    val status: String,
)

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

private data class TossErrorResponse(
    val code: String?,
    val message: String?,
)

class TossPaymentException(
    val status: HttpStatusCode?,
    val code: String?,
    message: String,
) : RuntimeException(message)
