package com.coco.payment.service

import com.coco.payment.persistence.enumerator.DiscountType
import com.coco.payment.persistence.model.InvoiceDiscount
import com.coco.payment.persistence.repository.InvoiceDiscountRepository
import org.springframework.stereotype.Service

@Service
class InvoiceDiscountService(
    private val invoiceDiscountRepository: InvoiceDiscountRepository,
    private val couponService: CouponService,
    private val pointService: PointService
) {

    fun create(
        invoiceSeq: Long,
        type: DiscountType,
        refSeq: Long,
        amount: Long,
    ) {

    }

    fun findByInvoiceSeq(invoiceSeq: Long): List<InvoiceDiscount> {
        TODO("Not yet implemented")
    }

    fun checkDiscountIsHold(invoiceId: Long) {
        val discounts = findByInvoiceSeq(invoiceId)

        // 쿠폰 할인 검증
        val couponDiscounts = discounts.filter { it.type == DiscountType.COUPON }
        if (couponDiscounts.isNotEmpty()) {
            couponDiscounts.forEach { discount ->
                if (!couponService.isCouponHoldValid(discount.refSeq)) {
                    throw IllegalStateException("유효하지 않은 쿠폰이 포함되어 있습니다.")
                }
            }
        }

        // 포인트 할인 검증
        val pointDiscounts = discounts.filter { it.type == DiscountType.POINT }
        if (pointDiscounts.isNotEmpty()) {
            pointDiscounts.forEach { discount ->
                if (!pointService.isPointHoldValid(discount.refSeq)) {
                    throw IllegalStateException("유효하지 않은 포인트가 포함되어 있습니다.")
                }
            }
        }
    }
}