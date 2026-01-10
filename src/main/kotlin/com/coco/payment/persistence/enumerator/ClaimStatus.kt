package com.coco.payment.persistence.enumerator

enum class ClaimStatus(val description: String) {
    REQUESTED("반품요청"),
    COLLECTING("수거중"),
    INSPECTING("검수중"),
    COMPLETED("반품확정"), // 검수 통과, 환불 대기
    REFUNDED("환불완료"), // 돈까지 돌려줌
    REJECTED("반품거절"),
    CANCELLED("취소")
}