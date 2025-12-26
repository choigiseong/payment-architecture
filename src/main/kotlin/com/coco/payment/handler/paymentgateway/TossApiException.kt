package com.coco.payment.handler.paymentgateway

class TossApiException(
    val status: Int,
    val code: String,
    override val message: String
) : RuntimeException(message)
