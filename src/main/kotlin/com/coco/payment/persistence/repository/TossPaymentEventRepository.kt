package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.TossPaymentEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TossPaymentEventRepository : JpaRepository<TossPaymentEvent, Long> {
}
