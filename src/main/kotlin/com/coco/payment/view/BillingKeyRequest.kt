package com.coco.payment.view

data class BillingKeyRequest(
    val customerKey: String,
    val authKey: String
) {
}