package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.SubscriptionStatus
import com.coco.payment.persistence.model.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findByCustomerSeq(customerSeq: Long): Subscription?
    fun findByNextBillingDateAndStatus(nextBillingDate: LocalDate, status: SubscriptionStatus): List<Subscription>
    fun findByStatus(status: SubscriptionStatus): List<Subscription>

    @Modifying
    @Query(
        value = "UPDATE subscription SET next_billing_date = :periodEnd WHERE id = :id",
        nativeQuery = true
    )
    fun renewSubscription(id: Long, periodEnd: LocalDate): Long

    @Modifying
    @Query(
        value = "UPDATE subscription SET status = :toStatus WHERE id = :id AND status IN (:fromStatus)",
        nativeQuery = true
    )
    fun cancel(id: Long, fromStatus: Set<SubscriptionStatus>, toStatus: SubscriptionStatus): Long
}
