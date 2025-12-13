package com.coco.payment.handler.dto

import com.coco.payment.persistence.enumerator.PaymentSystem

interface ConfirmBillingResponse {
    val paymentSystem: PaymentSystem
}
