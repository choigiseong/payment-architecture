package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.CustomerPaymentBillingKey
import com.coco.payment.service.dto.BillingKeyDto
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val customerService: CustomerService,
) {

    fun registerBillingKey(
        customerKey: String, billingKeyDto: BillingKeyDto
    ) {
        val customer = customerService.findByCustomerKey(customerKey)
        customerService.addBillingKey(
            customer.id,
            billingKeyDto,
        )
    }

    fun findBillingKey(customerKey: String, paymentSystem: PaymentSystem): CustomerPaymentBillingKey? {
        val customer = customerService.findByCustomerKey(customerKey)
        return customer.billingKeys.find { it.paymentSystem == paymentSystem }
    }

}