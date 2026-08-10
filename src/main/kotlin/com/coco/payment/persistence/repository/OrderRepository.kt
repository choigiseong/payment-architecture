package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.Order
import com.coco.payment.persistence.enumerator.OrderStatus
import org.apache.ibatis.annotations.Param

interface OrderRepository {
    fun insert(order: Order): Int

    fun findById(@Param("id") id: Long): Order?

    fun findByOrderKey(@Param("orderKey") orderKey: String): Order?

    fun findByOrderKeyForUpdate(@Param("orderKey") orderKey: String): Order?

    fun mark(@Param("id") id: Long, @Param("fromStatus") fromStatus: OrderStatus, @Param("toStatus") toStatus: OrderStatus): Int
}
