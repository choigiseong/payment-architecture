package com.coco.payment.service

import com.coco.payment.service.dto.PrepaymentView
import org.springframework.stereotype.Service

@Service
class CouponService {
    fun holdCoupon(couponList: List<PrepaymentView.CouponDiscountView>): List<PrepaymentView.CouponHoldResult> {
        TODO("Not yet implemented")
    }
}