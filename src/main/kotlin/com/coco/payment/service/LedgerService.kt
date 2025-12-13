package com.coco.payment.service

import com.coco.payment.persistence.model.Ledger
import com.coco.payment.persistence.repository.LedgerRepository
import org.springframework.stereotype.Service

@Service
class LedgerService(
    private val ledgerRepository: LedgerRepository
) {

    fun createLedger(
        customerSeq: Long
    ): Ledger {
        return ledgerRepository.save(
            Ledger(
                null,
                customerSeq,
                0,
                0,
                0,
            )
        )
    }
}