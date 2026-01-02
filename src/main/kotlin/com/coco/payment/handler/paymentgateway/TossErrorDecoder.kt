package com.coco.payment.handler.paymentgateway

import com.coco.payment.exception.TossApiException
import com.coco.payment.handler.paymentgateway.dto.TossPaymentView
import com.fasterxml.jackson.databind.ObjectMapper
import feign.Response
import feign.codec.ErrorDecoder

class TossErrorDecoder(
    private val objectMapper: ObjectMapper
) : ErrorDecoder {

    override fun decode(methodKey: String, response: Response): Exception {
        val body = response.body()?.asInputStream()?.readBytes()?.toString(Charsets.UTF_8)

        val errorResponse = runCatching {
            objectMapper.readValue(body, TossPaymentView.TossErrorResponse::class.java)
        }.getOrNull()

        return TossApiException(
            status = response.status(),
            code = errorResponse?.error?.code ?: "",
            message = errorResponse?.error?.message ?: ""
        )
    }
}