package com.coco.payment.controller.dto

data class BillingKeyRequest(
    val customerKey: String,
    val authKey: String
) {
}