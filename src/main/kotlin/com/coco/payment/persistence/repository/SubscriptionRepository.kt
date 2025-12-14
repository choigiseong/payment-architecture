package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.SubscriptionStatus
import com.coco.payment.persistence.model.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findByCustomerSeq(customerSeq: Long): Subscription?
    fun findByNextBillingDate(now: LocalDate, status: SubscriptionStatus): List<Subscription>
    fun findByStatus(status: SubscriptionStatus): List<Subscription>
}
