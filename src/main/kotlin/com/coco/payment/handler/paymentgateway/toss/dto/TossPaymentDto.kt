package com.coco.payment.handler.paymentgateway.toss.dto

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.client.ClientHttpResponse
import java.time.Instant
import java.time.OffsetDateTime

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
    val approvedAt: Instant?,
)

data class TossTransaction(
    val tid: String,
    val orderId: String,
    val status: String,
    val amount: Long,
)

data class TossErrorResponse(val code: String?, val message: String?)

data class TossBillingKeyIssueRequest(val customerKey: String, val authKey: String)

data class TossBillingKeyIssueResponse(val billingKey: String, val customerKey: String)

data class TossBillingPaymentRequest(
    val customerKey: String,
    val orderId: String,
    val orderName: String,
    val amount: Long,
)

data class TossBillingPaymentResponse(
    val paymentKey: String,
    val orderId: String,
    val totalAmount: Long,
    val status: String,
    val approvedAt: OffsetDateTime?,
)

data class TossPaymentInquiryResponse(
    val paymentKey: String,
    val orderId: String,
    val totalAmount: Long,
    val status: String,
    val approvedAt: OffsetDateTime?,
)

data class TossTransactionResponse(
    val transactionKey: String,
    val paymentKey: String,
    val orderId: String,
    val status: String,
    val amount: Long,
)

data class TossPaymentCancelRequest(val cancelReason: String)

class TossPaymentException(
    val code: String?,
    message: String,
) : RuntimeException(message) {
    companion object {
        // Toss는 오류 본문에 {"code": "...", "message": "..."} 형태로 사유를 준다.
        // 본문을 읽지 않으면 HTTP 상태 코드만 남아 "401"처럼 원인을 알 수 없는 값이 저장된다.
        fun from(objectMapper: ObjectMapper, clientResponse: ClientHttpResponse, fallbackMessage: String): TossPaymentException {
            val error = runCatching { objectMapper.readValue(clientResponse.body, TossErrorResponse::class.java) }.getOrNull()
            return TossPaymentException(
                code = error?.code ?: clientResponse.statusCode.value().toString(),
                message = error?.message ?: "$fallbackMessage: ${clientResponse.statusCode}",
            )
        }
    }
}
