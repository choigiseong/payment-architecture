package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.ReconciliationDiscrepancy

// 대사는 넣기만 한다. 읽기와 종결(status 갱신)은 관리자가 H2 콘솔에서 직접 한다.
interface ReconciliationDiscrepancyRepository {
    fun insert(discrepancy: ReconciliationDiscrepancy): Int
}
