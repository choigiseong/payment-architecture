package com.coco.payment.service.dto

import com.coco.payment.persistence.enumerator.PaymentSystem
import java.time.Instant

interface BillingView {

    data class BillingKeyResult(
        val paymentSystem: PaymentSystem,
        val billingKey: String,
        val cardNumber: String,
        val cardCompany: String
    )

    data class ConfirmBillingCommand(
        val billingKey: String,
        val customerSeq: Long,
        val paymentSystem: PaymentSystem,
        val amount: Long,
        val customerEmail: String,
        val customerName: String,
        val orderId: String,
        val orderName: String
    )

    interface ConfirmResult {
        val paymentSystem: PaymentSystem

        data class TossConfirmResult(
            override val paymentSystem: PaymentSystem = PaymentSystem.TOSS,
            val paymentKey: String,
            val type: String,
            val mId: String,
            val lastTransactionKey: String,
            val orderId: String,
            val totalAmount: Long,
            val balanceAmount: Long,
            val status: String,
            val requestedAt: Instant,
            val approvedAt: Instant,
            val taxFreeAmount: Long
        ) : ConfirmResult
    }

    data class RefundBillingCommand(
//        val customerSeq: Long,
        val originalTransactionKey: String,
        val paymentSystem: PaymentSystem,
        val amount: Long,
        val reason: String
    )

    interface RefundResult {
        val paymentSystem: PaymentSystem

        data class TossRefundResult(
            override val paymentSystem: PaymentSystem = PaymentSystem.TOSS,
            val paymentKey: String,
            val type: String,
            val mId: String,
            val lastTransactionKey: String,
            val orderId: String,
            val totalAmount: Long,
            val balanceAmount: Long,
            val status: String,
            val requestedAt: Instant,
            val approvedAt: Instant,
            val taxFreeAmount: Long,
            val cancelList: List<TossRefundInfo>,
        ) : RefundResult {
            override fun isRefundable(): Boolean {
                return this.balanceAmount != 0L
            }

            fun getLastCanceledInfo(): TossRefundInfo {
                return cancelList.find { it.transactionKey == lastTransactionKey }
                    ?: throw IllegalArgumentException("TossRefundResult not found lastTransactionKey, transactionKey: $lastTransactionKey")
            }
        }

        data class TossRefundInfo(
            val amount: Long,
            val canceledAt: Instant,
            val transactionKey: String
        )

        fun isRefundable(): Boolean
    }

    interface TransactionResult {
        val paymentSystem: PaymentSystem

        data class TossTransactionResult(
            override val paymentSystem: PaymentSystem = PaymentSystem.TOSS,
            val paymentKey: String,
            val type: String,
            val mId: String,
            val lastTransactionKey: String,
            val orderId: String,
            val totalAmount: Long,
            val balanceAmount: Long,
            val status: String,
            val requestedAt: Instant,
            val approvedAt: Instant,
            val taxFreeAmount: Long
        ) : TransactionResult {

            override fun toConfirmResult(): ConfirmResult.TossConfirmResult {
                return ConfirmResult.TossConfirmResult(
                    this.paymentSystem,
                    this.paymentKey,
                    this.type,
                    this.mId,
                    this.lastTransactionKey,
                    this.orderId,
                    this.totalAmount,
                    this.balanceAmount,
                    this.status,
                    this.requestedAt,
                    this.approvedAt,
                    this.taxFreeAmount,
                )
            }
        }

        fun toConfirmResult(): ConfirmResult
    }


}