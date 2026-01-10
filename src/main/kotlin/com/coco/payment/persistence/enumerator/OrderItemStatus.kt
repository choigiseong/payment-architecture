package com.coco.payment.persistence.enumerator

enum class OrderItemStatus(val description: String) {
    ORDERED("주문완료"),
    REFUND_PENDING("환불대기"), // 환불 요청 중
    REFUNDED("환불완료"),
    PARTIALLY_REFUNDED("부분환불"),
    CANCELLED("취소")
}