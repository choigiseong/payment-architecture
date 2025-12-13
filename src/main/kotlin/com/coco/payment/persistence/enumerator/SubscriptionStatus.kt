package com.coco.payment.persistence.enumerator

enum class SubscriptionStatus(val description: String) {
    ACTIVE("활성"),
    PAUSED("일시중지"),
    PAST_DUE("연체"),
    CANCELLED("해지")
}
