package com.coco.payment.service

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

}