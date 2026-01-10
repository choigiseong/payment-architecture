package com.coco.payment.service.facade

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

    // 인증 단계 메소드 작성
    // 쿠폰, 포인트에 대한 검증
    // 검증이 끝나면 invoice에 생성. 쿠폰, 포인트를 hold로 생성
    // 인증안하고 브라우저 닫을 수도 있으니, 스케줄러로 시간 지나면 닫아야함. 승인때 검증 실패로 유도해야함
    // 인증 단계: 사전 검증 후 인보이스/할인(Hold) 생성

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

        // todo 실패 시 hold했던 것들 풀어주고
        val confirmResult = paymentFacade.confirmPrepayment(
            invoice.id!!, PrepaymentView.ConfirmPrepaymentCommand(
                paymentSystem,
                pgTransactionKey,
                invoice.externalOrderKey,
                invoice.paidAmount,
            ), at
        )

        try {
            paymentFacade.successPrepayment(confirmResult)
        } catch (e: Exception) {
            // 실패 시 스케줄러/콜백이 보정함
//            log.error("Payment successful but business logic failed for invoice: ${invoice.id}", e)
        }
    }


    // todo 선결제 환불
    // 환불 시 배송비는 제외해야함
    // 부분환불 시 쿠폰은 item단위로
    fun refundPrepayment(
        invoiceId: Long,
        refundAmount: Long,
        reason: String,
        at: Instant
    ) {
        val invoice = invoiceService.findById(invoiceId)

        // 1. 환불 가능 금액 검증 및 시도 생성 (비관적 락 사용, 트랜잭션 커밋됨)
        refundAttemptService.createAttemptIfRefundable(
            invoiceSeq = invoiceId,
            requestAmount = refundAmount,
            reason = reason,
            at = at
        )

        // 2. PG사 환불 요청 (트랜잭션 없음)
        val refundResult = paymentFacade.requestRefundPrepaymentToPg(
            command = PrepaymentView.RefundPrepaymentCommand(
                originalTransactionKey = invoice.pgTransactionKey!!,
                paymentSystem = invoice.paymentSystem,
                amount = refundAmount,
                reason = reason
            )
        )

        try {
            // 3. 성공 처리 (트랜잭션)
            paymentFacade.successRefundPrepayment(refundResult, refundAmount)
        } catch (e: Exception) {
            // 환불 실패 시 로깅 및 예외 처리
            // 스케줄러/콜백을 통한 재시도 로직 필요
            throw IllegalStateException("환불 처리 중 오류가 발생했습니다.", e)
        }
    }
}