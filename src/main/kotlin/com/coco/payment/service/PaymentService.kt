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
        customerKey: String, billingKeyDto: BillingView.BillingKeyDto
    ) {
        val customer = customerService.findByCustomerKey(customerKey)
        customerService.addBillingKey(
            customer.id,
            billingKeyDto,
        )
    }

    fun findBillingKey(customerKey: String, paymentSystem: PaymentSystem): CustomerPaymentBillingKey? {
        val customer = customerService.findByCustomerKey(customerKey)
        return customer.billingKeys.find { it.paymentSystem == paymentSystem }
    }

    fun confirmBilling(
        customerKey: String,
        paymentSystem: PaymentSystem,
        confirmBillingDto: BillingView.ConfirmBillingDto
    ) {
        val billingKeyModel =
            findBillingKey(customerKey, paymentSystem)
                ?: throw IllegalArgumentException("Billing key not found")

        when (paymentSystem) {
            PaymentSystem.TOSS -> {
                tossPaymentService.confirmBilling(
                    customerKey,
                    billingKeyModel.billingKey,
                    confirmBillingDto.amount,
                    confirmBillingDto.customerEmail,
                    confirmBillingDto.customerName,
                    confirmBillingDto.orderId,
                    confirmBillingDto.orderName
                )
            }

            else -> {
                throw IllegalArgumentException("Payment system not found")
            }
        }

        //todo

    }


//    @Transactional
    fun successBilling() {

        // todo 이건 연결, 수정
        tossPaymentEventService.createTossPaymentEvent()
        ledgerService.createLedger()
    }

}