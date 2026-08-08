package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import com.coco.payment.persistence.model.PaymentTransaction
import com.coco.payment.persistence.repository.PaymentTransactionRepository
import org.springframework.stereotype.Service

@Service
class PaymentTransactionService(private val paymentTransactionRepository: PaymentTransactionRepository) {
    fun createPending(orderId: Long, moid: String, amount: Long): Long {
        val transaction = PaymentTransaction(null, orderId, moid, null, amount, PaymentTransactionStatus.PENDING, null, null)
        check(paymentTransactionRepository.insert(transaction) == 1) { "Failed to insert payment transaction" }
        return transaction.id!!
    }

    fun complete(paymentTransactionId: Long, tid: String) {
        check(paymentTransactionRepository.mark(paymentTransactionId, PaymentTransactionStatus.PENDING, PaymentTransactionStatus.SUCCESS, tid) == 1) {
            "Failed to mark payment transaction as successful"
        }
    }

    fun fail(paymentTransactionId: Long) {
        check(paymentTransactionRepository.mark(paymentTransactionId, PaymentTransactionStatus.PENDING, PaymentTransactionStatus.FAILED, null) == 1) {
            "Failed to mark payment transaction as failed"
        }
    }
}
