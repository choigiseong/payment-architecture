package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.Order
import com.coco.payment.persistence.enumerator.OrderStatus
import org.apache.ibatis.annotations.Param

interface OrderRepository {
    fun insert(order: Order): Int

    fun findById(@Param("id") id: Long): Order?

    fun updateStatus(@Param("id") id: Long, @Param("status") status: OrderStatus): Int
}
