package com.coco.payment.persistence.enumerator

enum class PaymentAttemptStatus(val description: String) {
    SUCCEEDED("성공"),
    FAILED("실패"),
    PENDING("대기"),
    TIMEOUT("결과 미확인")
}
