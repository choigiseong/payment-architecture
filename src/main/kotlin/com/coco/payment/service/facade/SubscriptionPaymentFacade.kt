package com.coco.payment.service.facade

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
        customer: Customer,
        uuid: UUID,
        at: Instant
    ) {
        val subscription = subscriptionService.findSubscriptionByCustomerSeq(customer.id!!)
        val invoice = invoiceService.findOrCreateCurrentSubscriptionInvoice(
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

        val billingKey = customer.findLastBillingKey(subscription.billingKey) ?: throw IllegalArgumentException("Billing key not found")
        val paymentSystem = billingKey.paymentSystem
        // todo try catch or retry해야한다. 연체..

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
        val successPayment = paymentAttemptService.findSuccessByInvoice(invoiceSeq)

        // todo 이게 맞나? pending 환불이 있는 경우는? 취소 금액으로 판정?
        val alreadyRefundedAmount = refundAttemptService.sumSuccessAmountByInvoice(invoiceSeq)
        val refundableAmount = invoice.refundableAmount(alreadyRefundedAmount)

        if (refundableAmount < refundAmount) {
            throw IllegalStateException("환불 가능 잔액 부족 (잔액: $refundableAmount)")
        }

        val refundResult = paymentFacade.refund(
            invoice.id!!,
            successPayment.id!!,
            now,
            BillingView.RefundBillingCommand(
                successPayment.pgTransactionKey!!,
                successPayment.paymentSystem,
                refundAmount,
                reason
            )
        )


        try {
            paymentFacade.successRefundBilling(
                refundResult
            )
        } catch (e: Exception) {
            // logging 결제는 성공했지만, 이후 비즈니스 로직 실패. 성공으로 응답.
            // 커버는 콜백과 스케줄러.
        }

    }

}