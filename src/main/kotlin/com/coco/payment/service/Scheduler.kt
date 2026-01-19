package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.dto.PgResult
import com.coco.payment.persistence.model.PaymentAttempt
import com.coco.payment.service.dto.BillingView
import com.coco.payment.service.facade.PaymentFacade
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class Scheduler(
    private val paymentFacade: PaymentFacade,
    private val invoiceService: InvoiceService,
    private val subscriptionService: SubscriptionService,
    private val paymentAttemptService: PaymentAttemptService,
) {

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    fun recoverPendingPayments() {
        val now = Instant.now()
        val paymentAttempts = paymentAttemptService.findPendingPaymentAttempts(now)
        for (attempt in paymentAttempts) {
            try {
                processPaymentRecovery(attempt, now)
            } catch (e: Exception) {
                // logging
            }
        }
    }

    private fun processPaymentRecovery(attempt: PaymentAttempt, now: Instant) {
        val invoice = invoiceService.findById(attempt.invoiceSeq)
        val result = paymentFacade.findTransaction(invoice.paymentSystem, invoice.externalOrderKey)
        when (result) {
            is PgResult.Success -> {
                paymentFacade.successBilling(
                    result.value.toConfirmResult()
                )
            }

            is PgResult.Fail -> {
                paymentFacade.failPayment(
                    invoice.id!!,
                    now,
                    result.error.message
                )
            }

            is PgResult.Retryable -> {
                invoiceService.updateLastAttemptAt(invoice.id!!, now)
            }

            is PgResult.Critical -> {
//                alarmService.notify(result.error)
            }
        }
    }

}