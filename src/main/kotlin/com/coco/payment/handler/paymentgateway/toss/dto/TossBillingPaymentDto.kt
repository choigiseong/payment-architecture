package com.coco.payment.handler.paymentgateway.toss.dto

data class TossBillingPaymentCommand(
    val billingKey: String,
    val customerKey: String,
    val moid: String,
    val orderName: String,
    val amount: Long,
)

data class TossBillingPaymentResult(
    val tid: String,
    val moid: String,
    val amount: Long,
)
