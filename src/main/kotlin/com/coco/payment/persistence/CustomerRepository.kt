package com.coco.payment.persistence

import com.coco.payment.persistence.model.Customer
import org.springframework.stereotype.Component

@Component
class CustomerRepository {

    private val db = mutableMapOf<Long, Customer>()


    fun save(customer: Customer) {
        db[customer.id] = customer
    }

    fun findById(customerId: Long): Customer? {
        return db[customerId]
    }

    fun findByCustomerKey(customerKey: String): Customer? {
        return db.values.find { it.getCustomerKey() == customerKey }
    }
}