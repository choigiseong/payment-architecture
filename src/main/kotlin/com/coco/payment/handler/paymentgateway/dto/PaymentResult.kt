package com.coco.payment.handler.paymentgateway.dto

sealed interface PaymentResult<out T> {
    data class Success<T>(val value: T) : PaymentResult<T>
    data class Failure(val error: PaymentError) : PaymentResult<Nothing>
    data class Unknown(val error: PaymentError) : PaymentResult<Nothing>
    data class PaymentError(val code: String?, val message: String) {
        // PG가 준 코드는 우리 어휘가 아니라 메시지에 원문으로 남긴다.
        val reason: String get() = listOfNotNull(code, message).joinToString(": ")
    }
}
