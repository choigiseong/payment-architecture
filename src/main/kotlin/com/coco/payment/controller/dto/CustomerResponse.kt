package com.coco.payment.controller.dto

import com.coco.payment.persistence.model.Customer

data class CustomerResponse(
    private val customer: Customer
) {
    val customerKey
        get() = customer.getCustomerKey()
}