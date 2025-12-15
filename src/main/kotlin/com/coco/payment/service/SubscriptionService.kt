package com.coco.payment.service

import com.coco.payment.persistence.enumerator.BillingCycle
import com.coco.payment.persistence.enumerator.SubscriptionStatus
import com.coco.payment.persistence.model.Subscription
import com.coco.payment.persistence.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
) {
    fun createSubscription(customerSeq: Long, amount: Long, cycle: BillingCycle, nextBillingDate: LocalDate) {
        subscriptionRepository.save(
            Subscription(
                null,
                "구독",
                customerSeq,
                amount,
                cycle,
                SubscriptionStatus.PAUSED,
                nextBillingDate,
            )
        )
    }

    fun findTodaySubscription(now: LocalDate): List<Subscription> {
        val subscriptions = subscriptionRepository.findByNextBillingDate(now, SubscriptionStatus.ACTIVE)
        val dueSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.PAST_DUE)
        return subscriptions + dueSubscriptions
    }

    fun findSubscriptionByCustomerSeq(customerSeq: Long): Subscription {
        return subscriptionRepository.findByCustomerSeq(customerSeq)
            ?: throw IllegalArgumentException("Subscription not found")
    }

    @Transactional
    fun renew(id: Long, periodEnd: LocalDate) {
        val affectedRows = subscriptionRepository.renewSubscription(
            id,
            periodEnd,
        )
        if (affectedRows != 1L) {
            throw IllegalArgumentException("Subscription not found")
        }

    }
}