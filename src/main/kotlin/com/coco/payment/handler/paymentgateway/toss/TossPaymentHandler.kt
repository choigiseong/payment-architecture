package com.coco.payment.handler.paymentgateway.toss

import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentCommand
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentResult
import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class TossPaymentHandler(
    private val tossRestClient: RestClient,
    private val objectMapper: ObjectMapper,
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
                throw toException(clientResponse, "Toss billing key issue failed")
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
                    throw toException(clientResponse, "Toss billing payment failed")
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
                    throw toException(clientResponse, "Toss payment inquiry failed")
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

    // 취소 대상 키는 승인 응답에만 들어 있으므로, 응답을 잃은 거래는 조회로 tid를 먼저 알아내야 한다.
    // 실패를 예외가 아니라 결과로 돌려 호출부가 다음 회차 재시도를 선택할 수 있게 한다.
    fun cancel(tid: String, cancelReason: String): PaymentResult<Unit> {
        return try {
            tossRestClient.post()
                .uri("/v1/payments/{paymentKey}/cancel", tid)
                .headers { headers -> headers.setBasicAuth(secretKey, "") }
                .contentType(MediaType.APPLICATION_JSON)
                .body(TossPaymentCancelRequest(cancelReason))
                .retrieve()
                .onStatus({ status -> status.isError }) { _, clientResponse ->
                    throw toException(clientResponse, "Toss payment cancel failed")
                }
                .toBodilessEntity()

            PaymentResult.Success(Unit)
        } catch (exception: TossPaymentException) {
            PaymentResult.Failure(PaymentResult.PaymentError(exception.code, exception.message ?: "Toss payment cancel failed"))
        } catch (exception: RestClientException) {
            PaymentResult.Unknown(PaymentResult.PaymentError(null, exception.message ?: "Toss payment cancel request failed"))
        }
    }

    // Toss는 오류 본문에 {"code": "...", "message": "..."} 형태로 사유를 준다.
    // 본문을 읽지 않으면 HTTP 상태 코드만 남아 "401"처럼 원인을 알 수 없는 값이 저장된다.
    private fun toException(clientResponse: ClientHttpResponse, fallbackMessage: String): TossPaymentException {
        val error = runCatching { objectMapper.readValue(clientResponse.body, TossErrorResponse::class.java) }.getOrNull()
        return TossPaymentException(
            code = error?.code ?: clientResponse.statusCode.value().toString(),
            message = error?.message ?: "$fallbackMessage: ${clientResponse.statusCode}",
        )
    }
}

private data class TossErrorResponse(val code: String?, val message: String?)

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

private data class TossPaymentCancelRequest(val cancelReason: String)

private class TossPaymentException(
    val code: String?,
    message: String,
) : RuntimeException(message)
