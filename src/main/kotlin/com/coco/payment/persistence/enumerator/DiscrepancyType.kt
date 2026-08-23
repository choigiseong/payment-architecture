package com.coco.payment.persistence.enumerator

enum class DiscrepancyType(val code: Int, val desc: String) {
    AMOUNT_MISMATCH(1, "금액 불일치"),
    CANCEL_FAILED(2, "취소 실패"),
    CANCELED_BUT_SUCCESS(3, "우리는 성공인데 PG는 취소"),
    ORPHAN(4, "우리 기록 없음"),
    MISSING_AT_PG(5, "우리는 성공인데 PG에 없음"),
    UNRESOLVED(6, "판정하지 못함"),
}
