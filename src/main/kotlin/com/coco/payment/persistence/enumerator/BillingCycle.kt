package com.coco.payment.persistence.enumerator

import java.time.LocalDate

enum class BillingCycle(val description: String) {
    DAILY("일간"),
    MONTHLY("월간"),
    YEARLY("년간");

    // 시작 포함, 끝 미포함
    fun getPeriodEnd(now: LocalDate): LocalDate {
        return when (this) {
            DAILY -> now.plusDays(1)
            MONTHLY -> now.plusMonths(1)
            YEARLY -> now.plusYears(1)
        }
    }
}
