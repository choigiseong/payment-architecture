package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.CustomerPaymentBillingKey
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.strategy.PaymentStrategyManager
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PaymentService(
    private val customerService: CustomerService,
    private val strategyManager: PaymentStrategyManager,
    private val paymentAttemptService: PaymentAttemptService,
) {

    fun registerBillingKey(
        customerKey: String, billingKeyResult: BillingView.BillingKeyResult
    ) {
        val customer = customerService.findByCustomerKey(customerKey)
        customerService.addBillingKey(
            customer.id!!,
            billingKeyResult,
        )
    }

    fun findBillingKey(customerSeq: Long, paymentSystem: PaymentSystem): CustomerPaymentBillingKey? {
        val customer = customerService.findById(customerSeq)
        return customer.billingKeys.find { it.paymentSystem == paymentSystem }
    }

    fun confirmBilling(
        invoiceSeq: Long,
        requestedAt: Instant,
        confirmBillingCommand: BillingView.ConfirmBillingCommand
    ): BillingView.ConfirmResult {
        val billingKeyModel =
            findBillingKey(confirmBillingCommand.customerSeq, confirmBillingCommand.paymentSystem)
                ?: throw IllegalArgumentException("Billing key not found")

        paymentAttemptService.createPaymentAttempt(
            invoiceSeq,
            confirmBillingCommand.paymentSystem,
            requestedAt
        )

        val strategy = strategyManager.resolve(confirmBillingCommand.paymentSystem)
        val result = strategy.confirmBilling(
            billingKeyModel.billingKey, confirmBillingCommand
        )
        // todo paymentAttemptService 상태 업데이트? db 장애는?

        return result
    }


    fun successBilling(
        customerSeq: Long,
        confirmResult: BillingView.ConfirmResult
    ) {
        val strategy = strategyManager.resolve(confirmResult.paymentSystem)
        strategy.onSuccess(customerSeq, confirmResult)
    }

}
