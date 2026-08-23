package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentFailCode
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

    fun findPendingDueForCheck(approveDoneBefore: Instant) =
        paymentTransactionRepository.findPendingDueForCheck(PaymentTransactionStatus.PENDING, approveDoneBefore)

    fun findByMoid(moid: String) = paymentTransactionRepository.findByMoid(moid)

    fun findPendingsCreatedBetween(from: Instant, to: Instant) =
        paymentTransactionRepository.findByStatusAndCreatedAtBetween(PaymentTransactionStatus.PENDING, from, to)

    fun findSuccessesApprovedBetween(from: Instant, to: Instant) =
        paymentTransactionRepository.findByStatusAndApprovedAtBetween(PaymentTransactionStatus.SUCCESS, from, to)

    @Transactional
    fun createPending(paymentKey: String, orderId: Long, moid: String, amount: Long): Long {
        val transaction = PaymentTransaction(null, paymentKey, orderId, moid, null, amount, PaymentTransactionStatus.PENDING, null, null, null, null, null)
        check(paymentTransactionRepository.insert(transaction) == 1) { "Failed to insert payment transaction" }
        return transaction.id!!
    }

    @Transactional
    fun complete(paymentTransactionId: Long, tid: String, approvedAt: Instant?) {
        check(paymentTransactionRepository.mark(paymentTransactionId, PaymentTransactionStatus.PENDING, PaymentTransactionStatus.SUCCESS, tid, approvedAt, null, null) == 1) {
            "Failed to mark payment transaction as successful"
        }
    }

    // 사유가 컬럼 길이를 넘겨 UPDATE가 실패하면 이미 청구된 결제를 FAILED로 확정하지 못하므로 잘라서 넣는다.
    @Transactional
    fun fail(paymentTransactionId: Long, failCode: PaymentFailCode, failMessage: String?, tid: String? = null) {
        val marked = paymentTransactionRepository.mark(
            paymentTransactionId,
            PaymentTransactionStatus.PENDING,
            PaymentTransactionStatus.FAILED,
            tid,
            null,
            failCode,
            failMessage?.take(500),
        )
        check(marked == 1) { "Failed to mark payment transaction as failed" }
    }
}
