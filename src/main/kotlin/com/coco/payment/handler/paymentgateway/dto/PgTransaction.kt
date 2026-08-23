package com.coco.payment.handler.paymentgateway.dto

import com.coco.payment.persistence.enumerator.PgPaymentStatus

// 대사가 보는 PG 거래. PG별 응답을 각 핸들러가 이 모양으로 접어서 돌려준다.
data class PgTransaction(
    val tid: String,
    val orderId: String,
    val status: PgPaymentStatus,
    val rawStatus: String,
    val amount: Long,
) {
    val isPaid: Boolean get() = status == PgPaymentStatus.PAID

    val isCanceled: Boolean get() = status == PgPaymentStatus.CANCELED
}
