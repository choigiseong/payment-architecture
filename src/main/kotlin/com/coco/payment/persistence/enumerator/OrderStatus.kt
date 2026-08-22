package com.coco.payment.persistence.enumerator

enum class OrderStatus(val code: Int, val desc: String) {
    PENDING_PAYMENT(1, "결제 대기"),
    PAID(2, "결제 완료"),
    PAYMENT_FAILED(3, "결제 실패"),
    PREPARING(4, "상품 준비 중"),
}
