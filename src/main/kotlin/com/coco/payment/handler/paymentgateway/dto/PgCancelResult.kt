package com.coco.payment.handler.paymentgateway.dto

import java.time.Instant

data class PgCancelResult(
    val transactionKey: String,
    val canceledAt: Instant,
)
