package com.coco.payment.service.dto

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

    )

    interface ConfirmResult {

        data class TossConfirmResult(

        ): ConfirmResult
    }


}