package com.coco.payment.service

import com.coco.payment.persistence.enumerator.CancelStatus
import com.coco.payment.persistence.model.PaymentCancel
import com.coco.payment.persistence.repository.PaymentCancelRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentCancelService(private val paymentCancelRepository: PaymentCancelRepository) {
    @Transactional
    fun request(paymentTransactionSeq: Long, reason: String) {
        val cancel = PaymentCancel(null, paymentTransactionSeq, reason, CancelStatus.REQUESTED, null, null, null, null, null)
        check(paymentCancelRepository.insert(cancel) == 1) { "Failed to insert payment cancel" }
    }
}
