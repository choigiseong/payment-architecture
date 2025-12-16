package com.coco.payment.persistence.enumerator

enum class InvoiceStatus(val description: String) {
    PENDING("대기"),
    DUE("청구중"), // 연체?
    PAID("결제완료"),
    FAILED("결제실패"),
    CANCELLED("취소");

    fun isPaid(): Boolean {
        return this == PAID
    }

}
