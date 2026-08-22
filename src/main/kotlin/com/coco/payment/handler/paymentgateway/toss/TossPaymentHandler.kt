package com.coco.payment.handler.paymentgateway.toss

import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingKeyIssueRequest
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingKeyIssueResponse
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentCommand
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentRequest
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentResponse
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentResult
import com.coco.payment.handler.paymentgateway.toss.dto.TossPaymentCancelRequest
import com.coco.payment.handler.paymentgateway.toss.dto.TossPaymentException
import com.coco.payment.handler.paymentgateway.toss.dto.TossPaymentInquiryResponse
import com.coco.payment.handler.paymentgateway.toss.dto.TossTransactionResponse
import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import com.coco.payment.handler.paymentgateway.dto.PgTransaction
import com.coco.payment.persistence.enumerator.PgPaymentStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.LocalDateTime

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
                throw TossPaymentException.from(objectMapper, clientResponse, "Toss billing key issue failed")
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
                    throw TossPaymentException.from(objectMapper, clientResponse, "Toss billing payment failed")
                }
                .body(TossBillingPaymentResponse::class.java)
                ?: return PaymentResult.Unknown(PaymentResult.PaymentError(null, "Toss billing payment response is empty"))

            PaymentResult.Success(
                TossBillingPaymentResult(
                    tid = response.paymentKey,
                    moid = response.orderId,
                    amount = response.totalAmount,
                    approvedAt = response.approvedAt?.toInstant(),
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
                    throw TossPaymentException.from(objectMapper, clientResponse, "Toss payment inquiry failed")
                }
                .body(TossPaymentInquiryResponse::class.java)
                ?: return PaymentResult.Unknown(PaymentResult.PaymentError(null, "Toss payment inquiry response is empty"))

            when (response.status) {
                "DONE" -> PaymentResult.Success(
                    TossBillingPaymentResult(tid = response.paymentKey, moid = response.orderId, amount = response.totalAmount, approvedAt = response.approvedAt?.toInstant())
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
                    throw TossPaymentException.from(objectMapper, clientResponse, "Toss payment cancel failed")
                }
                .toBodilessEntity()

            PaymentResult.Success(Unit)
        } catch (exception: TossPaymentException) {
            PaymentResult.Failure(PaymentResult.PaymentError(exception.code, exception.message ?: "Toss payment cancel failed"))
        } catch (exception: RestClientException) {
            PaymentResult.Unknown(PaymentResult.PaymentError(null, exception.message ?: "Toss payment cancel request failed"))
        }
    }

    // 시각은 서울 벽시계다 — Toss API가 존 없는 로컬 시각을 받는다.
    fun transactions(startDate: LocalDateTime, endDate: LocalDateTime): List<PgTransaction> {
        val all = mutableListOf<PgTransaction>()
        var cursor: String? = null
        do {
            val page = tossRestClient.get()
                .uri { builder ->
                    builder.path("/v1/transactions")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .queryParam("limit", TRANSACTIONS_PAGE_LIMIT)
                        .apply { cursor?.let { queryParam("startingAfter", it) } }
                        .build()
                }
                .headers { headers -> headers.setBasicAuth(secretKey, "") }
                .retrieve()
                .onStatus({ status -> status.isError }) { _, clientResponse ->
                    throw TossPaymentException.from(objectMapper, clientResponse, "Toss transactions inquiry failed")
                }
                .body(Array<TossTransactionResponse>::class.java)
                ?: throw TossPaymentException(null, "Toss transactions response is empty")
            all += page.map { PgTransaction(it.paymentKey, it.orderId, toPgPaymentStatus(it.status), it.status, it.amount) }
            cursor = page.lastOrNull()?.transactionKey
        } while (page.size == TRANSACTIONS_PAGE_LIMIT)
        return all
    }

    // inquiry()의 상태 분기와 같은 판정이다. Toss 어휘를 아는 곳은 이 핸들러뿐이어야 한다.
    private fun toPgPaymentStatus(status: String): PgPaymentStatus = when (status) {
        "DONE" -> PgPaymentStatus.PAID
        "CANCELED", "PARTIAL_CANCELED", "ABORTED", "EXPIRED" -> PgPaymentStatus.CANCELED
        else -> PgPaymentStatus.UNKNOWN
    }

    companion object {
        private const val TRANSACTIONS_PAGE_LIMIT = 5000
    }
}
