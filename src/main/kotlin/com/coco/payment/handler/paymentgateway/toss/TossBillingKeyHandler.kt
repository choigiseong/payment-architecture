package com.coco.payment.handler.paymentgateway.toss

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TossBillingKeyHandler(
    private val tossRestClient: RestClient,
    @Value("\${payment.toss.secret-key}")
    private val secretKey: String,
) {
    fun issue(customerKey: String, authKey: String): String {
        require(secretKey.isNotBlank()) { "TOSS_SECRET_KEY must be configured" }
        val response = tossRestClient.post()
            .uri("/v1/billing/authorizations/issue")
            .headers { headers -> headers.setBasicAuth(secretKey, "") }
            .contentType(MediaType.APPLICATION_JSON)
            .body(TossBillingKeyIssueRequest(customerKey, authKey))
            .retrieve()
            .onStatus({ status -> status.isError }) { _, clientResponse ->
                throw TossBillingKeyException("Toss billing key issue failed: ${clientResponse.statusCode}")
            }
            .body(TossBillingKeyIssueResponse::class.java)
            ?: throw TossBillingKeyException("Toss billing key issue response is empty")
        return response.billingKey
    }
}

private data class TossBillingKeyIssueRequest(val customerKey: String, val authKey: String)
private data class TossBillingKeyIssueResponse(val billingKey: String, val customerKey: String)
private class TossBillingKeyException(message: String) : RuntimeException(message)
