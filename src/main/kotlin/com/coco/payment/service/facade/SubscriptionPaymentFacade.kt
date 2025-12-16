package com.coco.payment.service.facade

import com.coco.payment.persistence.model.Customer
import com.coco.payment.service.CustomerService
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.PaymentService
import com.coco.payment.service.SubscriptionService
import com.coco.payment.service.dto.BillingView
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate


@Service
class SubscriptionPaymentFacade(
    private val subscriptionService: SubscriptionService,
    private val paymentService: PaymentService,
    private val invoiceService: InvoiceService,
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
            at,
        )
        if (invoice.isPaid()) {
            // 결제에 성공했었다.
            // 이미 과거에 결제가 성공했어야 nextBillingDate가 이동했어야 함.
            // 이상한 상태 logging후 알림.
            return
        }

        val billingKey = customer.findLastBillingKey() ?: throw IllegalArgumentException("Billing key not found")
        val paymentSystem = billingKey.paymentSystem
        //try catch or retry해야한다. 연체..



        // 타임 아웃 처리.. 성공 실패 알 수 없음에 대한 처리하자.
        val confirmResult = paymentService.confirmBilling(
            invoice.id!!,
            at,
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

        try {
            paymentService.successBilling(
                customer.id!!,
                confirmResult
            )
        } catch (e: Exception) {
            // logging 결제는 성공했지만, 이후 비즈니스 로직 실패. 성공으로 응답.
            // 커버는 콜백과 스케줄러.
        }
    }

    // 이걸로 비즈니스 예외 대응 한다.
    @Scheduled
    fun schedule() {
        // pending인 애 조회
        // 이후 토스 조회
        // 이후 업데이트
        val now = Instant.now()
//        invoiceService.findPendingInvoice()

    }
}