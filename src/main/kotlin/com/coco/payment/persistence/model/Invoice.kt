package com.coco.payment.persistence.model

import com.coco.payment.persistence.enumerator.InvoiceStatus
import com.coco.payment.persistence.enumerator.InvoiceType
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.Invoice
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
import java.util.UUID

// Invoice = 최종 결제 결과

// PaymentAttempt 실패
//≠ Invoice 실패

// 3번의 재시도는 PaymentAttempt에 3번 쌓이고, 그때동안 invoice는 PENDING 상태
// 3번의 재시도 후에도 실패하면 invoice는 FAILED 상태
@Entity
@Table(name = "invoice")
class Invoice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var type: InvoiceType,


    // 구독 결제 (추후 분리가 필요)
    @Column(nullable = true)
    val subscriptionSeq: Long? = null,
    @Column(nullable = true)
    val periodStart: LocalDate? = null,
    @Column(nullable = true)
    val periodEnd: LocalDate? = null,

    //간편 결제
    @Column(nullable = true)
    val orderSeq: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentSystem: PaymentSystem,
    @Column(nullable = false)
    var pgTransactionKey: String? = null,
    @Column(nullable = false)
    val totalAmount: Long,
    @Column(nullable = false)
    val paidAmount: Long,
    @Column(nullable = false)
    val discountAmount: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InvoiceStatus = InvoiceStatus.PENDING,
    @Column(nullable = true)
    var dueAt: Instant? = null, // todo 연체 시 사용 예정.
    @Column(nullable = true)
    var paidAt: Instant? = null,
    @Column(nullable = true)
    var failedAt: Instant? = null, // 최종 실패 시점 기록 추가
    @Column(nullable = true)
    var attemptCount: Int = 0,
    @Column(nullable = true)
    var lastAttemptAt: Instant, // todo 마지막 시도 시간 retry 개념 연체 시 사용 예정.
    @Column(nullable = false, unique = true)
    val externalOrderKey: String,
    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    fun isPaid(): Boolean {
        return status.isPaid()
    }

    fun refundableAmount(alreadyRefundedAmount: Long): Long {
        val remaining = paidAmount - alreadyRefundedAmount
        return remaining.coerceAtLeast(0)
    }

    companion object {
        fun buildExternalOrderKey(
            customerSeq: Long,
            type: InvoiceType,
            refSeq: Long,
            uuid: UUID
        ): String {
            return "invoice-${customerSeq}-${type}-${refSeq}-${uuid}"
        }

        fun ofPrepayment(
            orderSeq: Long,
            totalAmount: Long,
            paidAmount: Long,
            totalDiscount: Long,
            paymentSystem: PaymentSystem,
            externalOrderKey: String,
            at: Instant
        ): Invoice {
            return Invoice(
                type = InvoiceType.PREPAYMENT,
                orderSeq = orderSeq,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                discountAmount = totalDiscount,
                paymentSystem = paymentSystem,
                externalOrderKey = externalOrderKey,
                lastAttemptAt = at,
            )
        }

        fun ofSubscription(
            subscriptionSeq: Long,
            totalAmount: Long,
            periodStart: LocalDate,
            periodEnd: LocalDate,
            paymentSystem: PaymentSystem,
            externalOrderKey: String,
            at: Instant
        ): Invoice {
            return Invoice(
                type = InvoiceType.SUBSCRIPTION,
                subscriptionSeq = subscriptionSeq,
                orderSeq = null,
                totalAmount = totalAmount,
                paidAmount = totalAmount,
                discountAmount = 0,
                periodStart = periodStart,
                periodEnd = periodEnd,
                paymentSystem = paymentSystem,
                externalOrderKey = externalOrderKey,
                lastAttemptAt = at,
            )
        }
    }
}
