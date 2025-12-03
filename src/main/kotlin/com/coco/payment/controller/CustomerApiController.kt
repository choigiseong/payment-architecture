package com.coco.payment.controller

import com.coco.payment.persistence.model.Customer
import com.coco.payment.service.CustomerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/customers")
class CustomerApiController(
    private val customerService: CustomerService
) {

    // todo 이거 사용하도록 하고 client도 result로 받도록 수정
    @GetMapping("/me")
    fun getCustomer(): ResponseEntity<Customer> {
        val customer = customerService.findCustomerById(1)
        return ResponseEntity.ok(customer)
    }
}