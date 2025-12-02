package com.coco.payment.service.dto

data class BillingKeyDto(
    val billingKey: String,
    val cardNumber: String,
    val cardCompany: String
) {
}