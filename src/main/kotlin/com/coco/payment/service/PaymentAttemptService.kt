package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.PaymentAttempt
import com.coco.payment.persistence.repository.PaymentAttemptRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PaymentAttemptService(
    private val paymentAttemptRepository: PaymentAttemptRepository
) {

    fun createPaymentAttempt(
        invoiceSeq: Long,
        paymentSystem: PaymentSystem,
        requestedAt: Instant,
    ) {
        paymentAttemptRepository.save(
            PaymentAttempt(
                invoiceSeq = invoiceSeq,
                paymentSystem = paymentSystem,
                requestedAt = requestedAt
            )
        )
    }
}