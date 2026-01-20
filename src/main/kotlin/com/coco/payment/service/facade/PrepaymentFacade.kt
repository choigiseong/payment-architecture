package com.coco.payment.service.facade

import com.coco.payment.handler.paymentgateway.dto.PgError
import com.coco.payment.handler.paymentgateway.dto.PgResult
import com.coco.payment.persistence.enumerator.DiscountType
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.Customer
import com.coco.payment.persistence.model.Invoice
import com.coco.payment.service.CouponService
import com.coco.payment.service.InvoiceDiscountService
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.PaymentAttemptService
import com.coco.payment.service.PointService
import com.coco.payment.service.RefundAttemptService
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.dto.PrepaymentView
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class PrepaymentFacade(
    private val couponService: CouponService,
    private val pointService: PointService,
    private val invoiceService: InvoiceService,
    private val invoiceDiscountService: InvoiceDiscountService,
    private val paymentFacade: PaymentFacade,
    private val refundAttemptService: RefundAttemptService,
    private val paymentAttemptService: PaymentAttemptService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 인증 단계 메소드 작성
    // 쿠폰, 포인트에 대한 검증
    // 검증이 끝나면 invoice에 생성. 쿠폰, 포인트를 hold로 생성
    // 인증안하고 브라우저 닫을 수도 있으니, 스케줄러로 시간 지나면 닫아야함. 승인때 검증 실패로 유도해야함
    // 인증 단계: 사전 검증 후 인보이스/할인(Hold) 생성

    // todo 여기서 중복 차단
    @Transactional
    fun authorizePrepayment(
        customer: Customer,
        paymentSystem: PaymentSystem,
        orderSeq: Long,
        totalAmount: Long,
        couponList: List<PrepaymentView.CouponDiscountCommand>,
        pointDiscount: PrepaymentView.PointDiscountCommand,
        uuid: UUID,
        at: Instant
    ): Invoice {
//        require(totalAmount >= 0) { "totalAmount must be >= 0" }
//        require(couponDiscountAmount >= 0 && pointDiscountAmount >= 0) { "discount amounts must be >= 0" }

        // 정책이 늘어난다면, 전략패턴으로. 지금은 이른 듯
        val couponHoldResults = PrepaymentView.Coupons(
            couponService.holdCoupon(couponList)
        )
        val pointHoldResult = if (pointDiscount.isUsePoint()) {
            pointService.holdPoint(pointDiscount)
        } else null

        val summary = PrepaymentView.PaymentSummary.of(totalAmount, couponHoldResults, pointHoldResult)

        val invoice = invoiceService.createPrepaymentInvoice(
            paymentSystem, customer.id!!, orderSeq, summary, uuid, at
        )

        for (coupon in couponHoldResults.coupons) {
            invoiceDiscountService.create(invoice.id!!, DiscountType.COUPON, coupon.couponSeq, coupon.amount)

        }
        // 포인트 처리 방식을 더 좋게할 수 없나
        if (pointHoldResult != null) {
            invoiceDiscountService.create(
                invoice.id!!, DiscountType.POINT, pointHoldResult.pointTransactionSeq, pointHoldResult.amount
            )
        }

        return invoice
    }


    fun confirmPrepayment(
        externalOrderKey: String, pgTransactionKey: String, paymentSystem: PaymentSystem, amount: Long, at: Instant
    ) {
        // 1. 상태 검증 (이미 결제되었거나 취소된 건인지)
        val invoice = invoiceService.findByExternalOrderKey(externalOrderKey)
        if (invoice.isPaid()) return

        // 금액 대조 (보안상 필수)
        require(invoice.paidAmount == amount) { "결제 금액이 일치하지 않습니다." }
        invoiceDiscountService.checkDiscountIsHold(invoice.id!!)

        // 2. 결제 시도 생성 및 PG 요청 (PaymentFacade 내부에서 처리)
        val pgResult = paymentFacade.confirmPrepayment(
            invoice.id!!,
            PrepaymentView.ConfirmPrepaymentCommand(
                paymentSystem,
                pgTransactionKey,
                invoice.externalOrderKey,
                invoice.paidAmount,
            ),
            at
        )

        when (pgResult) {
            is PgResult.Success -> handleConfirmSuccess(invoice.id!!, pgResult.value)
            is PgResult.Fail -> handleConfirmFail(invoice.id!!, at, pgResult.error)
            is PgResult.Retryable -> {
                // 재시도 가능한 오류는 스케줄러가 처리하도록 둡니다. (별도 처리 없음)
            }
            is PgResult.Critical -> handleConfirmCritical(invoice.id!!, at, pgResult.error)
        }
    }

    private fun handleConfirmSuccess(invoiceId: Long, confirmResult: PrepaymentView.ConfirmResult) {
        try {
            paymentFacade.successPrepayment(confirmResult)
        } catch (e: Exception) {
            log.error("Payment successful but business logic failed for invoice: $invoiceId. This will be recovered by scheduler.", e)
        }
    }

    private fun handleConfirmFail(invoiceId: Long, at: Instant, pgError: PgError) {
        val errorMessage = pgError.message
        try {
            paymentFacade.failPayment(invoiceId, at, errorMessage)
        } catch (e: Exception) {
            log.error("Failed to update payment status to FAIL for invoice: $invoiceId after PG failure. This will be recovered by scheduler.", e)
        }
        throw IllegalStateException("결제 승인에 실패했습니다: $errorMessage")
    }

    private fun handleConfirmCritical(invoiceId: Long, at: Instant, pgError: PgError) {
        val errorMessage = pgError.message
        try {
            paymentFacade.failPayment(invoiceId, at, errorMessage)
        } catch (e: Exception) {
            log.error("Failed to update payment status to CRITICAL for invoice: $invoiceId after PG critical failure. This will be recovered by scheduler.", e)
        }
        // alarmService.notify(pgError) // PgError 객체를 직접 전달
        log.error("CRITICAL PAYMENT ERROR for invoice $invoiceId: $errorMessage")
        throw IllegalStateException("결제 승인 중 치명적인 오류가 발생했습니다: $errorMessage")
    }


    // todo 선결제 환불
    // 환불 시 배송비는 제외해야함
    // 부분환불 시 쿠폰은 item단위로
    fun refundPrepayment(
        invoiceId: Long,
        refundAmount: Long,
        reason: String,
        at: Instant,
        refundItems: List<PrepaymentView.RefundItemCommand>,
        claimSeq: Long? = null
    ) {
        val invoice = invoiceService.findById(invoiceId)

        // 1. 환불 가능 금액 검증, 시도 생성 및 PG 요청 (PaymentFacade 내부에서 처리)
        val refundResult = paymentFacade.refundPrepayment(
            invoiceSeq = invoiceId,
            at = at,
            command = PrepaymentView.RefundPrepaymentCommand(
                originalTransactionKey = invoice.pgTransactionKey!!,
                paymentSystem = invoice.paymentSystem,
                amount = refundAmount,
                reason = reason,
                refundItems = refundItems
            ),
            claimSeq = claimSeq
        )

        try {
            // 2. 성공 처리 (트랜잭션)
            paymentFacade.successRefundPrepayment(refundResult, refundAmount)
        } catch (e: Exception) {
            // 환불 실패 시 로깅 및 예외 처리
            // 스케줄러/콜백을 통한 재시도 로직 필요
            throw IllegalStateException("환불 처리 중 오류가 발생했습니다.", e)
        }
    }
}