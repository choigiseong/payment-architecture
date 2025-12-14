package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.PaymentAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentAttemptRepository : JpaRepository<PaymentAttempt, Long> {}
