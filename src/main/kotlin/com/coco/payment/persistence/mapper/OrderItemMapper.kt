package com.coco.payment.persistence.mapper

import com.coco.payment.persistence.model.OrderItem
import org.apache.ibatis.annotations.Param

interface OrderItemMapper {
    fun insert(orderItem: OrderItem): Int

    fun findById(@Param("id") id: Long): OrderItem?
}
