package com.coco.payment.persistence.enumerator

enum class OrderStatus(val code: Int) {
    PENDING_PAYMENT(1),
    PAID(2),
}
