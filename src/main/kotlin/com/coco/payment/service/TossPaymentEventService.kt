package com.coco.payment.service

import com.coco.payment.persistence.model.TossPaymentEvent
import com.coco.payment.persistence.repository.TossPaymentEventRepository
import com.coco.payment.service.dto.BillingView
import org.springframework.stereotype.Service

@Service
class TossPaymentEventService(
    private val tossPaymentEventRepository: TossPaymentEventRepository
) {
    fun createTossPaymentEvent(
        customerSeq: Long,
        ledgerSeq: Long,
        confirmBillingResult: BillingView.ConfirmResult.TossConfirmResult
    ): TossPaymentEvent {
        return tossPaymentEventRepository.save(
            TossPaymentEvent(
                null,
                customerSeq,
                ledgerSeq,
                confirmBillingResult.paymentKey,
                confirmBillingResult.type,
            )
        )
    }
}