package com.coco.payment.service

import com.coco.payment.persistence.enumerator.DiscrepancyStatus
import com.coco.payment.persistence.enumerator.DiscrepancyType
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import com.coco.payment.persistence.enumerator.PgPaymentStatus
import com.coco.payment.persistence.model.ReconciliationDiscrepancy
import com.coco.payment.persistence.repository.ReconciliationDiscrepancyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReconciliationDiscrepancyService(
    private val reconciliationDiscrepancyRepository: ReconciliationDiscrepancyRepository,
) {
    // 적재는 언제나 OPEN이다. 종결은 관리자가 하므로 호출부가 상태를 고를 일이 없다.
    // 맥락이 컬럼 길이를 넘겨 INSERT가 실패하면 불일치 자체가 사라지므로 잘라서 넣는다.
    @Transactional
    fun create(
        paymentSystem: PaymentSystem,
        type: DiscrepancyType,
        moid: String,
        ourStatus: PaymentTransactionStatus?,
        pgStatus: PgPaymentStatus?,
        ourAmount: Long?,
        pgAmount: Long?,
        detail: String?,
    ) {
        val discrepancy = ReconciliationDiscrepancy(
            null, paymentSystem, type, DiscrepancyStatus.OPEN, moid,
            ourStatus, pgStatus, ourAmount, pgAmount, detail?.take(500), null, null,
        )
        check(reconciliationDiscrepancyRepository.insert(discrepancy) == 1) { "Failed to insert reconciliation discrepancy" }
    }
}
