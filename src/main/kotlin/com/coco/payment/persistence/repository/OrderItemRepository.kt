package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.OrderItemStatus
import com.coco.payment.persistence.model.OrderItem
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    fun findByOrderSeq(orderSeq: Long): List<OrderItem>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :id")
    fun findByIdWithLock(id: Long): Optional<OrderItem>

    @Modifying
    @Query("UPDATE OrderItem oi SET oi.status = :toStatus WHERE oi.id = :id AND oi.status IN :fromStatus")
    fun updateStatus(id: Long, fromStatus: Set<OrderItemStatus>, toStatus: OrderItemStatus): Int
}