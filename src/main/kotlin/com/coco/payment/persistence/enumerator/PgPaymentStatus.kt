package com.coco.payment.persistence.enumerator

// 판정 어휘. PG 상태의 미러가 아니라서 PG가 늘어도 값이 늘지 않는다. 원문은 불일치의 detail에 남긴다.
enum class PgPaymentStatus(val code: Int, val desc: String) {
    UNKNOWN(0, "판정 불가"),
    PAID(1, "결제 완료"),
    CANCELED(2, "취소됨"),
}
