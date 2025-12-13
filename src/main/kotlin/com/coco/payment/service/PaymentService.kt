package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.CustomerPaymentBillingKey
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.strategy.PaymentStrategyManager
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val customerService: CustomerService,
    private val strategyManager: PaymentStrategyManager
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

    fun findBillingKey(customerKey: String, paymentSystem: PaymentSystem): CustomerPaymentBillingKey? {
        val customer = customerService.findByCustomerKey(customerKey)
        return customer.billingKeys.find { it.paymentSystem == paymentSystem }
    }

    fun confirmBilling(
        confirmBillingCommand: BillingView.ConfirmBillingCommand
    ): BillingView.ConfirmResult {
        val billingKeyModel =
            findBillingKey(confirmBillingCommand.customerKey, confirmBillingCommand.paymentSystem)
                ?: throw IllegalArgumentException("Billing key not found")

        val strategy = strategyManager.resolve(confirmBillingCommand.paymentSystem)
        return strategy.confirmBilling(
            billingKeyModel.billingKey, confirmBillingCommand
        )
    }


    fun successBilling(
        customerKey: String,
        confirmResult: BillingView.ConfirmResult
    ) {
        val customer = customerService.findByCustomerKey(customerKey)
        val strategy = strategyManager.resolve(confirmResult.paymentSystem)
        strategy.onSuccess(customer.id!!, confirmResult)
    }

}
