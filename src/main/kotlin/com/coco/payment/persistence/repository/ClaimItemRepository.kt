package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.ClaimItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ClaimItemRepository : JpaRepository<ClaimItem, Long> {
    fun findByClaimSeq(claimSeq: Long): List<ClaimItem>
}