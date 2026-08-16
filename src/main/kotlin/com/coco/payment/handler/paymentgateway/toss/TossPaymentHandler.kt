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
class TossPaymentHandler(
    private val tossRestClient: RestClient,
    @Value("\${payment.toss.secret-key}")
    private val secretKey: String,
) {
    // 설정 누락은 결제 시점이 아니라 기동 시점에 드러나야 한다.
    init {
        check(secretKey.isNotBlank()) { "TOSS_SECRET_KEY must be configured" }
    }

    // 발급은 돈이 움직이지 않아 "불확실"이라는 상태가 없다. 그래서 승인/조회와 달리
    // PaymentResult로 감싸지 않고 실패하면 예외를 던진다.
    fun issue(customerKey: String, authKey: String): String {
        val response = tossRestClient.post()
            .uri("/v1/billing/authorizations/issue")
            .headers { headers -> headers.setBasicAuth(secretKey, "") }
            .contentType(MediaType.APPLICATION_JSON)
            .body(TossBillingKeyIssueRequest(customerKey, authKey))
            .retrieve()
            .onStatus({ status -> status.isError }) { _, clientResponse ->
                throw TossPaymentException(
                    code = clientResponse.statusCode.value().toString(),
                    message = "Toss billing key issue failed: ${clientResponse.statusCode}",
                )
            }
            .body(TossBillingKeyIssueResponse::class.java)
            ?: throw TossPaymentException(null, "Toss billing key issue response is empty")
        return response.billingKey
    }

    fun approve(command: TossBillingPaymentCommand): PaymentResult<TossBillingPaymentResult> {
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

    fun inquiry(moid: String): PaymentResult<TossBillingPaymentResult> {
        return try {
            val response = tossRestClient.get()
                .uri("/v1/payments/orders/{orderId}", moid)
                .headers { headers -> headers.setBasicAuth(secretKey, "") }
                .retrieve()
                .onStatus({ status -> status.isError }) { _, clientResponse ->
                    throw TossPaymentException(
                        code = clientResponse.statusCode.value().toString(),
                        message = "Toss payment inquiry failed: ${clientResponse.statusCode}",
                    )
                }
                .body(TossPaymentInquiryResponse::class.java)
                ?: return PaymentResult.Unknown(PaymentResult.PaymentError(null, "Toss payment inquiry response is empty"))

            when (response.status) {
                "DONE" -> PaymentResult.Success(
                    TossBillingPaymentResult(tid = response.paymentKey, moid = response.orderId, amount = response.totalAmount)
                )
                "CANCELED", "PARTIAL_CANCELED", "ABORTED", "EXPIRED" ->
                    PaymentResult.Failure(PaymentResult.PaymentError(response.status, "Toss payment status: ${response.status}"))
                else ->
                    PaymentResult.Unknown(PaymentResult.PaymentError(response.status, "Toss payment still in progress: ${response.status}"))
            }
        } catch (exception: TossPaymentException) {
            PaymentResult.Unknown(PaymentResult.PaymentError(exception.code, exception.message ?: "Toss payment inquiry failed"))
        } catch (exception: RestClientException) {
            PaymentResult.Unknown(PaymentResult.PaymentError(null, exception.message ?: "Toss payment inquiry request failed"))
        }
    }
}

private data class TossBillingKeyIssueRequest(val customerKey: String, val authKey: String)

private data class TossBillingKeyIssueResponse(val billingKey: String, val customerKey: String)

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

private data class TossPaymentInquiryResponse(
    val paymentKey: String,
    val orderId: String,
    val totalAmount: Long,
    val status: String,
)

private class TossPaymentException(
    val code: String?,
    message: String,
) : RuntimeException(message)
