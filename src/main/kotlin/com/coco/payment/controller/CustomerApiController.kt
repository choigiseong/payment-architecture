package com.coco.payment.controller

import com.coco.payment.controller.dto.CustomerResponse
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

    @GetMapping("/me")
    fun getCustomerKey(): ResponseEntity<CustomerResponse> {
        val customer = customerService.findCustomerById(1)
        return ResponseEntity.ok(CustomerResponse(customer))
    }
}