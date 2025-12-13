package com.coco.payment.service

import com.coco.payment.handler.dto.TossPaymentView
import com.coco.payment.persistence.repository.TossPaymentEventRepository
import org.springframework.stereotype.Service

@Service
class TossPaymentEventService(
    private val tossPaymentEventRepository: TossPaymentEventRepository
) {
    fun createTossPaymentEvent(
        customerSeq: Long,
        confirmBillingResult: TossPaymentView.TossConfirmBillingBillingResponse
    ) {

    }
}