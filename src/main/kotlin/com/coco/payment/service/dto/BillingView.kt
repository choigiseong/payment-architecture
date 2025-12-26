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
            override fun isSuccess(): Boolean {
                return this.status == "PAID"
            }

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

        fun isSuccess(): Boolean
        fun toConfirmResult(): ConfirmResult
    }


}