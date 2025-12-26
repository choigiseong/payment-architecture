package com.coco.payment.service.strategy

import com.coco.payment.handler.paymentgateway.dto.PgResult
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.dto.BillingView

interface BillingPaymentStrategy {
    fun supports(): PaymentSystem
    fun confirmBilling(billingKey: String, command: BillingView.ConfirmBillingCommand): BillingView.ConfirmResult
    fun onSuccess(customerSeq: Long, confirmResult: BillingView.ConfirmResult)
    fun findTransaction(externalOrderKey: String): PgResult<BillingView.TransactionResult>
}
