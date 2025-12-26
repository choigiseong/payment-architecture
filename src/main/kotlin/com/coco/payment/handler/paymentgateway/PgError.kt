package com.coco.payment.handler.paymentgateway

data class PgError(
    val code: String,
    val message: String,
)