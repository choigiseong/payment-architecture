package com.coco.payment.service

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.CustomerPaymentBillingKey
import com.coco.payment.service.dto.BillingView
import org.springframework.stereotype.Service

@Service
class PaymentService(
    private val customerService: CustomerService,
    private val tossPaymentService: TossPaymentService,
    private val ledgerService: LedgerService,
    private val tossPaymentEventService: TossPaymentEventService
) {

    fun registerBillingKey(
        customerKey: String, billingKeyResult: BillingView.BillingKeyResult
    ) {
        val customer = customerService.findByCustomerKey(customerKey)
        customerService.addBillingKey(
            customer.id,
            billingKeyResult,
        )
    }

    fun findBillingKey(customerKey: String, paymentSystem: PaymentSystem): CustomerPaymentBillingKey? {
        val customer = customerService.findByCustomerKey(customerKey)
        return customer.billingKeys.find { it.paymentSystem == paymentSystem }
    }

    fun confirmBilling(
        confirmBillingDto: BillingView.ConfirmBillingDto
    ): BillingView.ConfirmBillingResult {
        val billingKeyModel =
            findBillingKey(confirmBillingDto.customerKey, confirmBillingDto.paymentSystem)
                ?: throw IllegalArgumentException("Billing key not found")

        return when (confirmBillingDto.paymentSystem) {
            PaymentSystem.TOSS -> {
                tossPaymentService.confirmBilling(
                    billingKeyModel.billingKey,
                    confirmBillingDto
                )
            }
            else -> {
                throw IllegalArgumentException("Payment system not found")
            }
        }
    }


    // 리스폰스가 request 처럼 사용되어야하는데.. 음...
    //    @Transactional
    fun successBilling(confirmBillingResult: BillingView.ConfirmBillingResult) {
        // 이건 인터페이스로, 다른 pg사도 할 수 있게 하고.
        tossPaymentEventService.createTossPaymentEvent()

        // 이건 공통 정보 넣고.
        ledgerService.createLedger()
    }

}