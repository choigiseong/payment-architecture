package com.coco.payment.persistence.enumerator

enum class BillingCycle(val description: String) {
    DAILY("일간"),
    MONTHLY("월간"),
    YEARLY("년간")
}
