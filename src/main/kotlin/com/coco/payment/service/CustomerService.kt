package com.coco.payment.service

import com.coco.payment.persistence.CustomerRepository
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.Customer
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

    fun addBillingKey(customerId: Long, paymentSystem: PaymentSystem, billingKey: String, cardNumber: String, cardCompany: String) {
        val customer = findCustomerById(customerId)
        customer.addBillingKey(paymentSystem, billingKey, cardNumber, cardCompany)
    }


}