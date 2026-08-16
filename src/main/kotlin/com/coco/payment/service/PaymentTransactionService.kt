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
        val transaction = PaymentTransaction(null, paymentKey, orderId, moid, null, amount, PaymentTransactionStatus.PENDING, null, null, expiredAt, null, null)
        check(paymentTransactionRepository.insert(transaction) == 1) { "Failed to insert payment transaction" }
        return transaction.id!!
    }

    @Transactional
    fun extendExpiry(id: Long, expiredAt: Instant) {
        check(paymentTransactionRepository.extendExpiry(id, expiredAt) == 1) { "Failed to extend payment transaction expiry" }
    }

    @Transactional
    fun complete(paymentTransactionId: Long, tid: String) {
        check(paymentTransactionRepository.mark(paymentTransactionId, PaymentTransactionStatus.PENDING, PaymentTransactionStatus.SUCCESS, tid, null, null) == 1) {
            "Failed to mark payment transaction as successful"
        }
    }

    // 사유가 컬럼 길이를 넘겨 UPDATE가 실패하면 이미 청구된 결제를 FAILED로 확정하지 못하므로 잘라서 넣는다.
    @Transactional
    fun fail(paymentTransactionId: Long, failCode: String?, failMessage: String?) {
        val marked = paymentTransactionRepository.mark(
            paymentTransactionId,
            PaymentTransactionStatus.PENDING,
            PaymentTransactionStatus.FAILED,
            null,
            failCode?.take(100),
            failMessage?.take(500),
        )
        check(marked == 1) { "Failed to mark payment transaction as failed" }
    }
}
