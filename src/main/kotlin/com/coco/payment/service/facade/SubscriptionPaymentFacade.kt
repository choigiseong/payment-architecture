package com.coco.payment.service.facade

import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.Customer
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.PaymentAttemptService
import com.coco.payment.service.RefundAttemptService
import com.coco.payment.service.SubscriptionService
import com.coco.payment.service.dto.BillingView
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID


@Service
class SubscriptionPaymentFacade(
    private val subscriptionService: SubscriptionService,
    private val paymentFacade: PaymentFacade,
    private val invoiceService: InvoiceService,
    private val refundAttemptService: RefundAttemptService,
    private val paymentAttemptService: PaymentAttemptService,
) {

    // todo
    // 구독 결제. 앞에서 이미 결제할 애들을 고른다
    // nextBillingDate이 오늘이고, 상태가 active인 애들
    // 결제 실패는 연체인 애들은 어떻게 결제할 것인가? 상태 관리는?

    fun paymentSubscribe(
        paymentSystem: PaymentSystem,
        customer: Customer,
        uuid: UUID,
        at: Instant
    ) {
        val subscription = subscriptionService.findSubscriptionByCustomerSeq(customer.id!!)
        val invoice = invoiceService.findOrCreateCurrentSubscriptionInvoice(
            paymentSystem,
            customer.id!!,
            subscription.id!!,
            subscription.nextBillingDate,
            subscription.cycle,
            subscription.amount,
            uuid,
            at,
        )
        if (invoice.isPaid()) {
            // 결제에 성공했었다.
            // 이미 과거에 결제가 성공했어야 nextBillingDate가 이동했어야 함.
            // 이상한 상태 logging후 알림.
            return
        }

        val billingKey = customer.findLastBillingKey(subscription.billingKey)
            ?: throw IllegalArgumentException("Billing key not found")
        val paymentSystem = billingKey.paymentSystem
        // todo try catch or retry해야한다. 연체..

        // 1. 결제 시도 생성 및 PG 요청 (PaymentFacade 내부에서 처리)
        val confirmResult = paymentFacade.confirmBilling(
            invoice.id!!,
            at,
            BillingView.ConfirmBillingCommand(
                billingKey.billingKey,
                customer.id!!,
                paymentSystem,
                invoice.paidAmount,
                customer.name,
                customer.name,
                invoice.externalOrderKey,
                subscription.name
            )
        )

        try {
            // 2. 성공 처리 (트랜잭션)
            paymentFacade.successBilling(
                confirmResult
            )
        } catch (e: Exception) {
            // logging 결제는 성공했지만, 이후 비즈니스 로직 실패. 성공으로 응답.
            // 커버는 콜백과 스케줄러.
        }
    }

    fun paymentRefund(
        invoiceSeq: Long,
        refundAmount: Long,
        reason: String,
    ) {
        val now = Instant.now()
        val invoice = invoiceService.findById(invoiceSeq)

        // 1. 환불 가능 금액 검증, 시도 생성 및 PG 요청 (PaymentFacade 내부에서 처리)
        val refundResult = paymentFacade.refundBilling(
            invoiceSeq = invoiceSeq,
            at = now,
            command = BillingView.RefundBillingCommand(
                invoice.pgTransactionKey!!,
                invoice.paymentSystem,
                refundAmount,
                reason
            )
        )


        try {
            // 2. 성공 처리 (트랜잭션)
            paymentFacade.successRefundBilling(
                refundResult
            )
        } catch (e: Exception) {
            // logging 결제는 성공했지만, 이후 비즈니스 로직 실패. 성공으로 응답.
            // 커버는 콜백과 스케줄러.
        }

    }

}