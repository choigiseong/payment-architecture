package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentSystem
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val customerService: CustomerService,
    private val tossPaymentService: TossPaymentService
) {

    fun registerBillingKey(
        paymentSystem: PaymentSystem,
        customerKey: String,
        authKey: String
    ) {
        val billingKeyDto = when(paymentSystem) {
            PaymentSystem.TOSS -> {
                tossPaymentService.issueBillingKey(
                    customerKey,
                    authKey
                )
            }
            else -> throw IllegalArgumentException("Unsupported payment system: $paymentSystem")
        }

        val customer = customerService.findByCustomerKey(customerKey)
        customerService.addBillingKey(
            customer.id,
            paymentSystem,
            billingKeyDto.billingKey,
            billingKeyDto.cardNumber,
            billingKeyDto.cardCompany
        )
    }

}