package com.coco.payment.controller

import com.coco.payment.controller.dto.TossBillingView
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.LedgerService
import com.coco.payment.service.PaymentService
import com.coco.payment.service.TossPaymentService
import com.coco.payment.service.dto.BillingView
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TossPaymentApiController(
    private val paymentService: PaymentService,
    private val tossPaymentService: TossPaymentService,
    private val ledgerService: LedgerService
) {

    private val paymentSystem = PaymentSystem.TOSS

    @RequestMapping(value = ["/issue-billing-key"])
    fun issueBillingKey(@RequestBody request: TossBillingView.BillingKeyRequest): ResponseEntity<TossBillingView.BillingKeyResponse> {
        val billingKeyResult = tossPaymentService.issueBillingKey(
            request.customerKey,
            request.authKey
        )
        paymentService.registerBillingKey(
            request.customerKey,
            billingKeyResult
        )

        val response = TossBillingView.BillingKeyResponse(
            billingKeyResult.billingKey,
            billingKeyResult.cardNumber,
            billingKeyResult.cardCompany,
        )
        return ResponseEntity.ok(response)
    }

    @RequestMapping(value = ["/confirm-billing"])
    fun confirmBilling(@RequestBody request: TossBillingView.ConfirmBillingRequest): ResponseEntity<TossBillingView.ConfirmBillingResponse> {
        val confirmBillingResult = paymentService.confirmBilling(
            BillingView.ConfirmBillingCommand(
                request.customerKey,
                paymentSystem,
                request.amount,
                request.customerEmail,
                request.customerName,
                request.orderId,
                request.orderName
            )
        )

        paymentService.successBilling(
            request.customerKey,
            confirmBillingResult
        )



        return ResponseEntity.ok(TossBillingView.ConfirmBillingResponse(request.orderId))
    }
}