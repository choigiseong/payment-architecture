package com.coco.payment.service

import com.coco.payment.persistence.CustomerRepository
import com.coco.payment.persistence.model.Customer
import com.coco.payment.service.dto.BillingView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomerService(
    private val customerRepository: CustomerRepository
) {

    fun createCustomer(customer: Customer) {
        customerRepository.save(customer)
    }

    fun findByCustomerKey(customerKey: String): Customer {
        val id = customerKey.removePrefix("customer-").toLongOrNull()
            ?: throw IllegalArgumentException("Invalid customer key")
        return customerRepository.findById(id).orElseThrow { IllegalArgumentException("Customer not found") }
    }

    fun findCustomerById(customerId: Long): Customer {
        return customerRepository.findById(customerId).orElseThrow { IllegalArgumentException("Customer not found") }
    }

    @Transactional
    fun addBillingKey(customerId: Long, billingKeyResult: BillingView.BillingKeyResult) {
        val customer = findCustomerById(customerId)
        customer.addBillingKey(
            billingKeyResult.paymentSystem,
            billingKeyResult.billingKey,
            billingKeyResult.cardNumber,
            billingKeyResult.cardCompany
        )
        // Ensure persistence when outside of dirty tracking or for clarity
        customerRepository.save(customer)
    }
}
