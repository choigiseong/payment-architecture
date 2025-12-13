package com.coco.payment.service.strategy

import com.coco.payment.persistence.enumerator.PaymentSystem
import org.springframework.stereotype.Service

@Service
class PaymentStrategyManager(
    private val strategies: List<PaymentStrategy>
) {
    fun resolve(system: PaymentSystem): PaymentStrategy =
        strategies.firstOrNull { it.supports() == system }
            ?: throw IllegalArgumentException("Payment strategy not found for $system")
}
