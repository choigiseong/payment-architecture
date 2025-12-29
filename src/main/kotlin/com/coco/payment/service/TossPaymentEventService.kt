package com.coco.payment.service

import com.coco.payment.persistence.model.TossPaymentEvent
import com.coco.payment.persistence.repository.TossPaymentEventRepository
import com.coco.payment.service.dto.BillingView
import org.springframework.stereotype.Service

@Service
class TossPaymentEventService(
    private val tossPaymentEventRepository: TossPaymentEventRepository
) {
    // todo 임시
    fun createTossPaymentEvent(
        customerSeq: Long,
        ledgerSeq: Long,
        transactionKey: String,
        type: String // todo 이것도 enum으로
    ): TossPaymentEvent {
        return tossPaymentEventRepository.save(
            TossPaymentEvent(
                null,
                customerSeq,
                ledgerSeq,
                transactionKey,
                type,
            )
        )
    }


}