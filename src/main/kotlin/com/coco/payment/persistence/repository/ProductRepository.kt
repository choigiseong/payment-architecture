package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.Product
import org.apache.ibatis.annotations.Param

interface ProductRepository {
    fun findAll(): List<Product>

    fun findByIds(@Param("ids") ids: List<Long>): List<Product>
}
