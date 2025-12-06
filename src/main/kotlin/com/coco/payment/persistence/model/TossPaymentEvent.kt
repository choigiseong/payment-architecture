package com.coco.payment.persistence.model

import java.time.Instant

class TossPaymentEvent(
    val id: Long,
    val customerSeq: String,
    val paymentKey: String,
    val orderId: String,
    val totalAmount: Long,
    val balanceAmount: Long,
    val status: String,
    val requestedAt: Instant,
    val approvedAt: Instant,
    val taxFreeAmount: Long
) {
}