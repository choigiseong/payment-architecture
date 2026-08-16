package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.OrderItem
import org.apache.ibatis.annotations.Param

interface OrderItemRepository {
    fun insert(orderItem: OrderItem): Int

    fun findById(@Param("id") id: Long): OrderItem?

    fun findByOrderSeq(@Param("orderSeq") orderSeq: Long): List<OrderItem>
}
