package com.coco.payment.service.facade

import com.coco.payment.persistence.enumerator.DiscountType
import com.coco.payment.persistence.enumerator.InvoiceStatus
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
        couponList: List<PrepaymentView.CouponDiscountView>,
        pointDiscount: PrepaymentView.PointDiscountView,
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

//    fun approvePrepayment(invoiceId: Long, approvedAt: Instant) {
//        val invoice = invoiceRepository.findById(invoiceId)
//            .orElseThrow { IllegalArgumentException("Invoice not found") }
//        val discounts = invoiceDiscountRepository.findByInvoiceSeq(invoiceId)
//
//        val discountSum = discounts.sumOf { it.amount }
//        require(discountSum == invoice.discountAmount) { "discount aggregate mismatch" }
//        require(invoice.totalAmount - discountSum == invoice.paidAmount) { "paidAmount mismatch" }
//
//        // TODO: (선택) PG 승인 API 호출 및 결과 검증 로직 삽입
//        // ex) paymentService.capture(...)
//
//        // 아랜 전략에서 저장처리
//
//        // 인보이스 승인(PAID) 처리
//        val affected = invoiceRepository.paid(
//            invoice.id!!,
//            approvedAt,
//            setOf(InvoiceStatus.PENDING),
//            InvoiceStatus.PAID
//        )
//        if (affected != 1L) {
//            throw IllegalStateException("Failed to approve invoice")
//        }
//
//        // TODO: (선택) 원장/이벤트 기록, 후속 비즈니스 처리 등
//    }
}