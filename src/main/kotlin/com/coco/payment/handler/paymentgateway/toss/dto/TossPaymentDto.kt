package com.coco.payment.handler.paymentgateway.toss.dto

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
) : RuntimeException(message)
