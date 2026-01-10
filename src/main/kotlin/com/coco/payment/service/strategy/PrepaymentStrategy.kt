package com.coco.payment.service.strategy

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.dto.PrepaymentView

interface PrepaymentStrategy {
    fun supports(paymentSystem: PaymentSystem): Boolean
    fun confirmPrepayment(command: PrepaymentView.ConfirmPrepaymentCommand): PrepaymentView.ConfirmResult
    fun onSuccessPrepayment(confirmResult: PrepaymentView.ConfirmResult)
    fun onSuccessRefundPrepayment(refundResult: BillingView.RefundResult, refundAmount: Long)
}