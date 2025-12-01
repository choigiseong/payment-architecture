package com.coco.payment.controller

import com.coco.payment.service.CustomerService
import com.coco.payment.view.BillingKeyRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentApiController(
    private val customerService: CustomerService,
    @Value("\${payment.toss.api-key}")
    private val tossPayApiKey: String
) {

    @RequestMapping(value = ["/issue-billing-key"])
    fun issueBillingKey(@RequestBody request: BillingKeyRequest): ResponseEntity<String> {
//        val requestData: JSONObject = parseRequestData(jsonBody)
//        val response: JSONObject = sendRequest(
//            requestData,
//            tossPayApiKey,
//            "https://api.tosspayments.com/v1/billing/authorizations/issue"
//        )
//
//        if (!response.containsKey("error")) {
//            billingKeyMap.put(requestData.get("customerKey"), response.get("billingKey"))
//        }

//        return ResponseEntity.status(if (response.containsKey("error")) 400 else 200).body<JSONObject?>(response)
        return ResponseEntity.ok().body("ok")
    }
}