package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import com.coco.payment.persistence.model.PaymentTransaction
import com.coco.payment.persistence.repository.PaymentTransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PaymentTransactionService(private val paymentTransactionRepository: PaymentTransactionRepository) {
    fun findById(id: Long) = paymentTransactionRepository.findById(id)

    fun findByPaymentKey(paymentKey: String) = paymentTransactionRepository.findByPaymentKey(paymentKey)

    fun findPendingByOrderSeq(orderSeq: Long) =
        paymentTransactionRepository.findByOrderSeqAndStatus(orderSeq, PaymentTransactionStatus.PENDING)

    fun findExpiredPending(now: Instant) =
        paymentTransactionRepository.findExpiredPending(PaymentTransactionStatus.PENDING, now)

    @Transactional
    fun createPending(paymentKey: String, orderId: Long, moid: String, amount: Long, expiredAt: Instant): Long {
        val transaction = PaymentTransaction(null, paymentKey, orderId, moid, null, amount, PaymentTransactionStatus.PENDING, expiredAt, null, null)
        check(paymentTransactionRepository.insert(transaction) == 1) { "Failed to insert payment transaction" }
        return transaction.id!!
    }

    @Transactional
    fun extendExpiry(id: Long, expiredAt: Instant) {
        check(paymentTransactionRepository.extendExpiry(id, expiredAt) == 1) { "Failed to extend payment transaction expiry" }
    }

    @Transactional
    fun complete(paymentTransactionId: Long, tid: String) {
        check(paymentTransactionRepository.mark(paymentTransactionId, PaymentTransactionStatus.PENDING, PaymentTransactionStatus.SUCCESS, tid) == 1) {
            "Failed to mark payment transaction as successful"
        }
    }

    @Transactional
    fun fail(paymentTransactionId: Long) {
        check(paymentTransactionRepository.mark(paymentTransactionId, PaymentTransactionStatus.PENDING, PaymentTransactionStatus.FAILED, null) == 1) {
            "Failed to mark payment transaction as failed"
        }
    }
}
