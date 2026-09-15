package com.coco.payment.persistence.enumerator

// 취소 작업의 상태. 실패는 상태가 아니다 — last_error만 갱신하고 REQUESTED로 남아 재시도한다.
enum class CancelStatus(val code: Int, val desc: String) {
    REQUESTED(1, "취소 요청"),
    DONE(2, "취소 완료"),
}
