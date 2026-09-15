package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import com.coco.payment.handler.paymentgateway.toss.TossPaymentHandler
import com.coco.payment.persistence.model.PaymentCancel
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PaymentCancelProcessService(
    private val paymentCancelService: PaymentCancelService,
    private val paymentTransactionService: PaymentTransactionService,
    private val tossPaymentHandler: TossPaymentHandler,
) {
    @Scheduled(fixedDelayString = "\${payment.cancel.interval-ms}")
    fun processRequestedCancels() {
        for (cancel in paymentCancelService.findRequested()) {
            try {
                process(cancel)
            } catch (exception: Exception) {
                log.error("Failed to process payment cancel: ${cancel.id}", exception)
            }
        }
    }

    private fun process(cancel: PaymentCancel) {
        val transaction = paymentTransactionService.findById(cancel.paymentTransactionSeq)
            ?: error("Payment transaction not found for cancel: ${cancel.id}")
        val tid = transaction.tid ?: error("Payment transaction has no tid for cancel: ${cancel.id}")
        when (val result = tossPaymentHandler.cancel(tid, cancel.reason, "payment-cancel-${cancel.id}")) {
            is PaymentResult.Success ->
                paymentCancelService.complete(cancel.id!!, result.value.transactionKey, result.value.canceledAt)
            is PaymentResult.Failure, is PaymentResult.Unknown ->
                paymentCancelService.recordFailure(cancel.id!!, result.errorOrNull?.reason)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PaymentCancelProcessService::class.java)
    }
}
