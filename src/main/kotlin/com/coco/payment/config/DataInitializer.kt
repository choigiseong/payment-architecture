package com.coco.payment.config

import com.coco.payment.persistence.repository.CustomerRepository
import com.coco.payment.persistence.model.Customer
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataInitializer {
    @Bean
    fun initCustomer(customerRepository: CustomerRepository): CommandLineRunner = CommandLineRunner {
        if (customerRepository.count() == 0L) {
            customerRepository.save(Customer(name = "test"))
        }
    }
}
