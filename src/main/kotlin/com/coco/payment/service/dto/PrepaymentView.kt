package com.coco.payment.service.dto

import com.coco.payment.persistence.enumerator.PaymentSystem
import java.time.Instant

interface PrepaymentView {

    data class CouponDiscountCommand(
        val couponSeq: Long,
        val orderItemSeq: Long,
    )

    data class PointDiscountCommand(
        val amount: Long,
    ) {
        fun isUsePoint(): Boolean {
            return amount > 0
        }
    }

    data class CouponHoldResult(
        val couponSeq: Long,
        val orderItemSeq: Long,
        val amount: Long,
    )

    data class PointHoldResult(
        val pointTransactionSeq: Long,
        val amount: Long,
    ) {
        fun isUsePoint(): Boolean {
            return amount > 0
        }
    }

    data class Coupons(
        val coupons: List<CouponHoldResult>,
    ) {
        fun getTotalAmount(): Long {
            return coupons.sumOf { it.amount }
        }

        fun isUseCoupon(): Boolean {
            return coupons.isNotEmpty()
        }
    }

    data class PaymentSummary(
        val totalAmount: Long,
        val couponDiscount: Long,
        val pointDiscount: Long
    ) {
        val paidAmount: Long = totalAmount - couponDiscount - pointDiscount
        val totalDiscount: Long = couponDiscount + pointDiscount

        init {
            require(paidAmount >= 0) { "결제 금액은 0원보다 작을 수 없습니다." }
        }

        companion object {
            fun of(total: Long, coupons: Coupons, point: PointHoldResult?): PaymentSummary {
                return PaymentSummary(
                    totalAmount = total,
                    couponDiscount = coupons.getTotalAmount(),
                    pointDiscount = point?.amount ?: 0
                )
            }
        }
    }

    data class ConfirmPrepaymentCommand(
        val paymentSystem: PaymentSystem,
        val pgTransactionKey: String,
        val externalOrderKey: String,
        val amount: Long,
    )

    interface ConfirmResult {
        val paymentSystem: PaymentSystem


        data class TossConfirmResult(
            override val paymentSystem: PaymentSystem,
            val paymentKey: String,
            val type: String,
            val mId: String,
            val lastTransactionKey: String,
            val orderId: String,
            val totalAmount: Long,
            val balanceAmount: Long,
            val status: String,
            val requestedAt: Instant,
            val approvedAt: Instant,
            val taxFreeAmount: Long,
        ) : ConfirmResult
    }


}