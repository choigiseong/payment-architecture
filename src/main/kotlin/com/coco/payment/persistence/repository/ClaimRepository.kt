package com.coco.payment.persistence.repository

import com.coco.payment.persistence.enumerator.ClaimStatus
import com.coco.payment.persistence.model.Claim
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ClaimRepository : JpaRepository<Claim, Long> {

    @Modifying
    @Query("UPDATE Claim c SET c.status = :toStatus WHERE c.id = :id AND c.status IN :fromStatus")
    fun updateStatus(id: Long, fromStatus: Set<ClaimStatus>, toStatus: ClaimStatus): Int
}