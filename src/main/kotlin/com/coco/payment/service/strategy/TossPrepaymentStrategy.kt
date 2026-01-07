package com.coco.payment.service.strategy

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.PaymentAttemptService
import com.coco.payment.service.TossPaymentService
import com.coco.payment.service.dto.PrepaymentView
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class TossPrepaymentStrategy(
    private val tossPaymentService: TossPaymentService,
    private val invoiceService: InvoiceService,
    private val paymentAttemptService: PaymentAttemptService,
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
        )

        paymentAttemptService.succeeded(
            invoice.id!!,
            confirmResult.approvedAt,
            confirmResult.paymentKey,
        )
    }

}