package com.coco.payment.service.dto

data class BillingPaymentCommand(
    val companySeq: Long,
    val moid: String,
    val orderName: String,
    val totalPrice: Long,
    val items: List<BillingOrderItem>,
)

data class PrepareBillingPaymentResult(
    val orderId: Long,
    val paymentTransactionId: Long,
    val billingKey: String,
    val customerKey: String,
    val moid: String,
    val orderName: String,
    val amount: Long,
)

data class BillingPaymentResult(
    val tid: String,
    val moid: String,
    val amount: Long,
)

sealed interface PaymentResult<out T> {
    data class Success<T>(val value: T) : PaymentResult<T>

    data class Failure(val error: PaymentError) : PaymentResult<Nothing>

    data class Unknown(val error: PaymentError) : PaymentResult<Nothing>

    data class PaymentError(
        val code: String?,
        val message: String,
    )
}

data class BillingOrderItem(
    val itemName: String,
    val unitPrice: Long,
    val quantity: Int,
)
