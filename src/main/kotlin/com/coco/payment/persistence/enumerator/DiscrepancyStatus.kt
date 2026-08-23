package com.coco.payment.persistence.enumerator

enum class DiscrepancyStatus(val code: Int, val desc: String) {
    OPEN(1, "확인 필요"),
    RESOLVED(2, "종결"),
}
