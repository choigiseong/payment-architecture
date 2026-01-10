package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.RefundAttemptItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RefundAttemptItemRepository : JpaRepository<RefundAttemptItem, Long> {
    fun findByRefundAttemptSeq(refundAttemptSeq: Long): List<RefundAttemptItem>
}