package com.coco.payment.service.facade

import com.coco.payment.persistence.model.Customer
import com.coco.payment.service.CustomerService
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.PaymentService
import com.coco.payment.service.SubscriptionService
import com.coco.payment.service.dto.BillingView
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate


@Service
class SubscriptionPaymentFacade(
    private val subscriptionService: SubscriptionService,
    private val paymentService: PaymentService,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
) {

    // 구독 결제. 앞에서 이미 결제할 애들을 고른다
    // nextBillingDate이 오늘이고, 상태가 active인 애들
    // 결제 실패는 연체인 애들은 어떻게 결제할 것인가? 상태 관리는?

    fun paymentSubscribe(
        customer: Customer,
        at: Instant
    ) {
        val subscription = subscriptionService.findSubscriptionByCustomerSeq(customer.id!!)
        val invoice = invoiceService.findOrCreateCurrent(
            customer.id!!,
            subscription.id!!,
            subscription.nextBillingDate,
            subscription.cycle,
            subscription.amount,
        )
        if (invoice.isPaid()) {
            // 결제에 성공했었다.
            // 이미 과거에 결제가 성공했어야 nextBillingDate가 이동했어야 함.
            // 이상한 상태 logging후 알림.
            return
        }

        val billingKey = customer.findLastBillingKey() ?: throw IllegalArgumentException("Billing key not found")
        val paymentSystem = billingKey.paymentSystem
        //try catch or retry해야한다. 연체
        paymentService.confirmBilling(
            // todo paymentattempt status에도 넣고하자.
            BillingView.ConfirmBillingCommand(
                customer.id!!,
                paymentSystem,
                invoice.amount,
                customer.name,
                customer.name,
                invoice.externalOrderKey,
                subscription.name
            )
        )
        // 뭔가..
        paymentService.successBilling()
    }
}