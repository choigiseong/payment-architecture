package com.coco.payment.config

import com.coco.payment.handler.paymentgateway.TossErrorDecoder
import com.fasterxml.jackson.databind.ObjectMapper
import feign.RequestInterceptor
import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Base64

@Configuration
@EnableFeignClients(basePackages = ["com.coco.payment"])
class FeignConfig(
    @Value("\${payment.toss.api-key}")
    private val apiKey: String
) {

    @Bean
    fun tossAuthInterceptor(): RequestInterceptor {
        return RequestInterceptor { template ->
            val encoded = Base64.getEncoder()
                .encodeToString("$apiKey:".toByteArray())
            template.header("Authorization", "Basic $encoded")
            template.header("Content-Type", "application/json")
        }
    }

    @Bean
    fun tossErrorDecoder(
        objectMapper: ObjectMapper
    ): ErrorDecoder = TossErrorDecoder(objectMapper)
}