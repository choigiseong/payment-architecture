package com.coco.payment.handler.paymentgateway.dto

sealed interface PaymentResult<out T> {
    data class Success<T>(val value: T) : PaymentResult<T>
    data class Failure(val error: PaymentError) : PaymentResult<Nothing>
    data class Unknown(val error: PaymentError) : PaymentResult<Nothing>
    data class PaymentError(val code: String?, val message: String)
}
