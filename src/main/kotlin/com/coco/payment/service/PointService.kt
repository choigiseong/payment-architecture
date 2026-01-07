package com.coco.payment.service

import com.coco.payment.service.dto.PrepaymentView
import org.springframework.stereotype.Service

@Service
class PointService {

    fun holdPoint(pointDiscountCommand: PrepaymentView.PointDiscountCommand): PrepaymentView.PointHoldResult {
        TODO("Not yet implemented")
    }

    fun isPointHoldValid(refSeq: Long?): Boolean {
        TODO("Not yet implemented")
    }
}