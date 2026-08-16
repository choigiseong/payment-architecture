package com.coco.payment.service

import com.coco.payment.persistence.model.Product
import com.coco.payment.persistence.repository.ProductRepository
import org.springframework.stereotype.Service

@Service
class ProductService(private val productRepository: ProductRepository) {
    fun findAll(): List<Product> = productRepository.findAll()

    fun findByIds(ids: List<Long>): Map<Long, Product> =
        productRepository.findByIds(ids.distinct()).associateBy { it.id!! }
}
