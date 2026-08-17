package com.coco.payment.persistence.enumerator

enum class PaymentTransactionStatus(val code: Int, val desc: String) {
    PENDING(1, "확인 중"),
    SUCCESS(2, "성공"),
    FAILED(3, "실패"),
}
