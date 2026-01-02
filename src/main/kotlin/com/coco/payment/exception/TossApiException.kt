package com.coco.payment.exception

class TossApiException(
    val status: Int,
    val code: String,
    override val message: String
) : RuntimeException(message)