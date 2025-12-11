package com.coco.payment.service

import com.coco.payment.persistence.CustomerRepository
import com.coco.payment.persistence.model.Customer
import com.coco.payment.service.dto.BillingView
import org.springframework.stereotype.Service

@Service
class CustomerService(
    private val customerRepository: CustomerRepository
) {

    fun createCustomer(customer: Customer) {
        customerRepository.save(customer)
    }

    fun findByCustomerKey(customerKey: String): Customer {
        return customerRepository.findByCustomerKey(customerKey) ?: throw IllegalArgumentException("Customer not found")
    }

    fun findCustomerById(customerId: Long): Customer {
        return customerRepository.findById(customerId) ?: throw IllegalArgumentException("Customer not found")
    }

    fun addBillingKey(customerId: Long, billingKeyResponse: BillingView.BillingKeyResponse) {
        val customer = findCustomerById(customerId)
        customer.addBillingKey(
            billingKeyResponse.paymentSystem,
            billingKeyResponse.billingKey,
            billingKeyResponse.cardNumber,
            billingKeyResponse.cardCompany
        )
    }


}