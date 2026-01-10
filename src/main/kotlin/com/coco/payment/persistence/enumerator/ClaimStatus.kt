package com.coco.payment.persistence.enumerator

enum class ClaimStatus(val description: String) {
    REQUESTED("반품요청"),
    COLLECTING("수거중"),
    INSPECTING("검수중"),
    COMPLETED("반품확정"), // 검수 통과, 환불 대기
    REFUND_PROCESSING("환불처리중"), // 스케줄러가 가져감
    REFUNDED("환불완료"), // 돈까지 돌려줌
    REFUND_FAILED("환불실패"), // 재시도 초과 등
    REJECTED("반품거절"),
    CANCELLED("취소")
}