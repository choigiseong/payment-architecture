package com.coco.payment.service.strategy

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.TossPaymentService
import com.coco.payment.service.dto.PrepaymentView
import org.springframework.stereotype.Service

@Service
class TossPrepaymentStrategy(
    private val tossPaymentService: TossPaymentService,
) : PrepaymentStrategy {
    override fun supports(paymentSystem: PaymentSystem): Boolean = paymentSystem == PaymentSystem.TOSS
    override fun confirmPrepayment(command: PrepaymentView.ConfirmPrepaymentCommand): PrepaymentView.ConfirmResult {
        return tossPaymentService.confirmPrepayment(command)
    }

    override fun onSuccessPrepayment(confirmResult: PrepaymentView.ConfirmResult) {
        TODO("Not yet implemented")
        // todo 이제 승인 성공했으니 업데이트 처리

//        val invoice = invoiceService.findById(result.invoiceSeq)
//
//        // 1. Invoice 상태 변경: PENDING -> PAID
//        invoice.markAsPaid(result.paidAt, result.pgTransactionKey)
//
//        // 2. 할인 자원(쿠폰, 포인트) Hold -> Use 상태로 확정
//        // InvoiceDiscount에 저장된 refSeq들을 이용해 각 서비스 호출
//        assetService.confirmAssets(invoice.id!!)
//
//        // 3. 주문 상태 변경 (이게 주문 도메인과의 접점)
//        orderService.completeOrder(invoice.orderSeq)

    }

}