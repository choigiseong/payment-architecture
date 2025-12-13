package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.Ledger
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LedgerRepository : JpaRepository<Ledger, Long> {
}
