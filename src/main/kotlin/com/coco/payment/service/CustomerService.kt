package com.coco.payment.service

import com.coco.payment.persistence.CustomerRepository
import org.springframework.stereotype.Service

@Service
class CustomerService(
    private val customerRepository: CustomerRepository
) {


}