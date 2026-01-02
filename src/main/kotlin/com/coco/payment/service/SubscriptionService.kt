package com.coco.payment.service

import com.coco.payment.persistence.enumerator.BillingCycle
import com.coco.payment.persistence.enumerator.SubscriptionStatus
import com.coco.payment.persistence.model.Subscription
import com.coco.payment.persistence.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Optional

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
) {


    fun findById(id: Long): Subscription {
        return subscriptionRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Subscription not found") }
    }

    fun findTodaySubscription(now: LocalDate): List<Subscription> {
        val subscriptions = subscriptionRepository.findByNextBillingDateAndStatus(now, SubscriptionStatus.ACTIVE)
        val dueSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.PAST_DUE)
        return subscriptions + dueSubscriptions
    }

    fun findSubscriptionByCustomerSeq(customerSeq: Long): Subscription {
        return subscriptionRepository.findByCustomerSeq(customerSeq)
            ?: throw IllegalArgumentException("Subscription not found")
    }


    fun createSubscription(customerSeq: Long, billingKey: String, amount: Long, cycle: BillingCycle, nextBillingDate: LocalDate) {
        subscriptionRepository.save(
            Subscription(
                null,
                "구독",
                customerSeq,
                billingKey,
                amount,
                cycle,
                SubscriptionStatus.PAUSED,
                nextBillingDate,
            )
        )
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

    @Transactional
    fun cancel(id: Long) {
        val affectedRows = subscriptionRepository.cancel(
            id,
            setOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED, SubscriptionStatus.PAST_DUE),
            SubscriptionStatus.CANCELLED
        )
        if (affectedRows != 1L) {
            throw IllegalArgumentException("Subscription not found")
        }
    }
}