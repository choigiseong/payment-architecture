package com.coco.payment.persistence.enumerator

enum class DiscrepancyType(val code: Int, val desc: String) {
    AMOUNT_MISMATCH(1, "금액 불일치"),
    CANCELED_BUT_SUCCESS(3, "우리는 성공인데 PG는 취소"),
    ORPHAN(4, "우리 기록 없음"),
    MISSING_AT_PG(5, "우리는 성공인데 PG에 없음"),
    UNRESOLVED(6, "판정하지 못함"),
    PAID_BUT_FAILED(7, "우리는 실패인데 PG는 승인"),
    STUCK_PENDING(8, "종결되지 않은 미결"),
}
