package com.coco.payment.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentApiController {

//    @RequestMapping(value = ["/issue-billing-key"])
//    fun issueBillingKey(@RequestBody jsonBody: String?): ResponseEntity<JSONObject> {
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
//
//        return ResponseEntity.status(if (response.containsKey("error")) 400 else 200).body<JSONObject?>(response)
//    }
}