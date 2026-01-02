package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.BillingCycle
import com.coco.payment.persistence.enumerator.SubscriptionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "subscription")
class Subscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var customerSeq: Long,
    @Column(nullable = false)
    var billingKey: String,
    @Column(nullable = false)
    var amount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var cycle: BillingCycle = BillingCycle.MONTHLY,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    @Column(nullable = false)
    var nextBillingDate: LocalDate,
    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
