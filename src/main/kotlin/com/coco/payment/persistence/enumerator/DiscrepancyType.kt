package com.coco.payment.persistence.enumerator

enum class DiscrepancyType(val code: Int, val desc: String) {
    AMOUNT_MISMATCH(1, "금액 불일치"),
    ORPHAN(4, "우리 기록 없음"),
    MISSING_AT_PG(5, "우리는 성공인데 PG에 없음"),
    UNRESOLVED(6, "판정하지 못함"),
    PAID_BUT_FAILED(7, "우리는 실패인데 PG는 승인"),
    STUCK_PENDING(8, "종결되지 않은 미결"),
    UNKNOWN_CANCEL(9, "우리가 모르는 취소"),
    STUCK_CANCEL(10, "종결되지 않은 취소"),
    CANCEL_MISSING_AT_PG(11, "취소했다는데 PG에 없음"),
}
