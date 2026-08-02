package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.Order
import org.apache.ibatis.annotations.Param

interface OrderRepository {
    fun insert(order: Order): Int

    fun findById(@Param("id") id: Long): Order?
}
