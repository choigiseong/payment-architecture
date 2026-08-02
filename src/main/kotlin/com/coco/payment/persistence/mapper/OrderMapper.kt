package com.coco.payment.persistence.mapper

import com.coco.payment.persistence.model.Order
import org.apache.ibatis.annotations.Param

interface OrderMapper {
    fun insert(order: Order): Int

    fun findById(@Param("id") id: Long): Order?
}
