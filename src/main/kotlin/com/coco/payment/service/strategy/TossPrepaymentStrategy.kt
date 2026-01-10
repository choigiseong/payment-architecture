package com.coco.payment.service.strategy

import com.coco.payment.persistence.enumerator.DiscountType
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.CouponService
import com.coco.payment.service.InvoiceDiscountService
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.PaymentAttemptService
import com.coco.payment.service.PointService
import com.coco.payment.service.RefundAttemptService
import com.coco.payment.service.TossPaymentService
import com.coco.payment.service.dto.PrepaymentView
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class TossPrepaymentStrategy(
    private val tossPaymentService: TossPaymentService,
    private val invoiceService: InvoiceService,
    private val paymentAttemptService: PaymentAttemptService,
    private val refundAttemptService: RefundAttemptService,
    private val invoiceDiscountService: InvoiceDiscountService,
    private val couponService: CouponService,
    private val pointService: PointService
) : PrepaymentStrategy {
    override fun supports(paymentSystem: PaymentSystem): Boolean = paymentSystem == PaymentSystem.TOSS
    override fun confirmPrepayment(command: PrepaymentView.ConfirmPrepaymentCommand): PrepaymentView.ConfirmResult {
        return tossPaymentService.confirmPrepayment(command)
    }

    @Transactional
    override fun onSuccessPrepayment(confirmResult: PrepaymentView.ConfirmResult) {
        if (confirmResult !is PrepaymentView.ConfirmResult.TossConfirmResult) {
            throw IllegalArgumentException("Provider response is not TossConfirmResult")
        }

        val invoice = invoiceService.findByExternalOrderKey(confirmResult.orderId)

        invoiceService.paid(
            invoice.id!!,
            confirmResult.approvedAt,
            confirmResult.paymentKey
        )

        paymentAttemptService.succeeded(
            invoice.id!!,
            confirmResult.approvedAt,
        )
    }

    @Transactional
    override fun onSuccessRefundPrepayment(refundResult: PrepaymentView.RefundResult, refundAmount: Long) {
        if (refundResult !is PrepaymentView.RefundResult.TossRefundResult) {
            throw IllegalArgumentException("Provider response is not TossRefundResult")
        }

        // 락을 걸고 Invoice 조회 (동시성 제어)
        val invoice = invoiceService.findByExternalOrderKey(refundResult.orderId)
        invoiceService.findByIdWithLock(invoice.id!!)

        val lastCanceledInfo = refundResult.getLastCanceledInfo()

        refundAttemptService.succeeded(
            invoice.id!!,
            lastCanceledInfo.canceledAt,
            lastCanceledInfo.transactionKey
        )

        // 현재까지 성공한 환불 총액 조회 (방금 성공 처리한 건 포함)
        val totalRefundedAmount = refundAttemptService.sumSucceededAmountByInvoice(invoice.id!!)
        val isFullRefund = (totalRefundedAmount >= invoice.paidAmount)

        if (isFullRefund) {
            invoiceService.refunded(invoice.id!!)
        } else {
            // 부분 환불 처리
            invoiceService.partiallyRefunded(invoice.id!!)

            // 쿠폰 부분 환불 처리 (쿠폰 금액 비율에 따라 계산)
            val refundRatio = refundAmount.toDouble() / invoice.paidAmount.toDouble()
            val invoiceDiscounts = invoiceDiscountService.findByInvoiceSeq(invoice.id!!)

            for (discount in invoiceDiscounts) {
                val refundDiscountAmount = (discount.amount * refundRatio).toLong()
                when (discount.type) {
                    DiscountType.COUPON -> {
                        // 쿠폰 부분 환불 처리
                        // couponService.refundCoupon(discount.refSeq!!, refundDiscountAmount)
                    }

                    DiscountType.POINT -> {
                        // 포인트 환불 처리
                        // pointService.refundPoint(discount.refSeq!!, refundDiscountAmount)
                    }
                }
            }
        }
    }

    override fun refundPrepayment(command: PrepaymentView.RefundPrepaymentCommand): PrepaymentView.RefundResult {
        return tossPaymentService.cancelPrepayment(command)
    }

}