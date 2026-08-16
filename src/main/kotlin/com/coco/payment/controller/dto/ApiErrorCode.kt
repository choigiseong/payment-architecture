package com.coco.payment.controller.dto

// ProblemDetail의 type에 실어 보내는 기계용 식별자. 클라이언트는 이 값으로 분기한다.
enum class ApiErrorCode(val value: Int) {
    INVALID_REQUEST(4000),
    ORDER_ALREADY_PAID(4091),
    DELIVERY_DATE_CHANGED(4092),
}
