package com.coco.payment.persistence.enumerator

// 거래가 실패로 끝난 사유. 우리 판단만 담고, PG가 준 코드와 메시지는 fail_message에 원문으로 남긴다.
enum class PaymentFailCode(val code: Int, val desc: String) {
    APPROVE_REJECTED(1, "승인 거절"),
    PG_CANCELED(2, "PG에서 취소된 결제"),
    NOT_CONFIRMED(3, "기한 안에 확인하지 못함"),
    NET_CANCEL(4, "기한 초과로 취소"),
    RECON_CANCEL(5, "대사가 뒤늦게 취소"),
    CANCEL_FAILED(6, "취소하지 못한 채 종결"),
}
