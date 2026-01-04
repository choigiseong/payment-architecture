package com.coco.payment.service.strategy

import com.coco.payment.persistence.enumerator.PaymentSystem
import org.springframework.stereotype.Service

@Service
class PaymentStrategyManager(
    private val billingPaymentStrategies: List<BillingPaymentStrategy>,
    private val prepaymentStrategies: List<PrepaymentStrategy>
) {
    fun billingPaymentResolve(system: PaymentSystem): BillingPaymentStrategy =
        billingPaymentStrategies.firstOrNull { it.supports(system) }
            ?: throw IllegalArgumentException("Payment strategy not found for $system")

    fun prepaymentPaymentResolve(system: PaymentSystem): PrepaymentStrategy =
        prepaymentStrategies.firstOrNull { it.supports(system) }
            ?: throw IllegalArgumentException("Payment strategy not found for $system")
}
