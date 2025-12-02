package com.coco.payment.config

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
    fun errorDecoder(): ErrorDecoder {
        return ErrorDecoder { methodKey, response -> // 여기서 예외를 처리하고 Result.failure()로 래핑
            when (response.status()) {
                400 -> IllegalArgumentException("잘못된 요청입니다.")
                401, 403 -> RuntimeException("인증에 실패했습니다.")
                else -> RuntimeException("결제 처리 중 오류가 발생했습니다.")
            }
        }
    }
}