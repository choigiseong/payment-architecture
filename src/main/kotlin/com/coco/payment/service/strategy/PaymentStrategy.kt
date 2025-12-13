package com.coco.payment.service.strategy

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.service.dto.BillingView
import com.coco.payment.handler.dto.ConfirmBillingResponse

interface PaymentStrategy {
    fun supports(): PaymentSystem
    fun confirmBilling(billingKey: String, command: BillingView.ConfirmBillingCommand): ConfirmBillingResponse
    fun onSuccess(customerId: Long, providerResponse: ConfirmBillingResponse)
}
