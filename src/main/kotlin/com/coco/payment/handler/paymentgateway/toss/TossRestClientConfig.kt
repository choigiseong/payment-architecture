package com.coco.payment.handler.paymentgateway.toss

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class TossRestClientConfig(
    @Value("\${payment.toss.base-url}")
    private val baseUrl: String,
) {
    @Bean
    fun tossRestClient(restClientBuilder: RestClient.Builder): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(10))
            setReadTimeout(Duration.ofSeconds(60))
        }

        return restClientBuilder
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
