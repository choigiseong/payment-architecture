package com.coco.payment.service.facade

import com.coco.payment.persistence.enumerator.DiscountType
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.Customer
import com.coco.payment.persistence.model.Invoice
import com.coco.payment.service.CouponService
import com.coco.payment.service.InvoiceDiscountService
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.PointService
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
    private val paymentFacade: PaymentFacade
) {

    // 인증 단계 메소드 작성
    // 쿠폰, 포인트에 대한 검증
    // 검증이 끝나면 invoice에 생성. 쿠폰, 포인트를 hold로 생성
    // 인증안하고 브라우저 닫을 수도 있으니, 스케줄러로 시간 지나면 닫아야함. 승인때 검증 실패로 유도해야함
    // 인증 단계: 사전 검증 후 인보이스/할인(Hold) 생성

    @Transactional
    fun authorizePrepayment(
        customer: Customer,
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
            customer.id!!,
            orderSeq,
            summary,
            uuid,
            at
        )

        for (coupon in couponHoldResults.coupons) {
            invoiceDiscountService.create(invoice.id!!, DiscountType.COUPON, coupon.couponSeq, coupon.amount)

        }
        // 포인트 처리 방식을 더 좋게할 수 없나
        if (pointHoldResult != null) {
            invoiceDiscountService.create(
                invoice.id!!,
                DiscountType.POINT,
                pointHoldResult.pointTransactionSeq,
                pointHoldResult.amount
            )
        }

        return invoice
    }


    // 승인 단계 메소드 작성
    // 인증때 생성한 거 검증
    // 승인 단계에서 api 치고
    // invoice기반으로 hold된 쿠폰, 포인트 사용 및 상태 업데이트
    // 그리고 상태 변경 쭉
    // 실패 시 hold했던 것들 풀어주고

    fun confirmPrepayment(
        externalOrderKey: String,
        paymentKey: String,
        paymentSystem: PaymentSystem,
        amount: Long,
        at: Instant
    ) {
        // 1. 상태 검증 (이미 결제되었거나 취소된 건인지)
        val invoice = invoiceService.findByExternalOrderKey(externalOrderKey)
        if (invoice.isPaid()) return

        // 금액 대조 (보안상 필수)
        require(invoice.paidAmount == amount) { "결제 금액이 일치하지 않습니다." }
        // 쿠폰 사용 가능? hold임?

        // 2. [API 단계] PG 승인 요청
        // Subscription과 마찬가지로 confirmBilling과 유사한 PG 승인 메서드 호출
        val confirmResult = paymentFacade.confirmPrepayment(
            invoice.id!!,
            invoice.externalOrderKey,
            paymentSystem,
            invoice.paidAmount,
            at
        )

        // 3. [성공 후 비즈니스 로직 단계]
        // 여기서부터는 DB 트랜잭션 영역이며, 실패 시 스케줄러/콜백이 보정함
        try {
            paymentFacade.successPrepayment(confirmResult)
        } catch (e: Exception) {
            // 결제는 성공했지만 내부 로직(주문완료 처리 등) 실패 시 로그 남김
            // 보정은 배치/스케줄러가 invoice.status == PENDING && PG승인여부=YES인 건을 찾아 처리
//            log.error("Payment successful but business logic failed for invoice: ${invoice.id}", e)
        }
    }

    // 환불 시 배송비는 제외해야함
    // 부분환불 시 쿠폰은 item단위로
}