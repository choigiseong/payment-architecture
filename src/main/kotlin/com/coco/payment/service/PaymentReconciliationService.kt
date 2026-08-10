package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import com.coco.payment.handler.paymentgateway.toss.TossBillingPaymentHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PaymentReconciliationService(
    private val paymentTransactionService: PaymentTransactionService,
    private val paymentWorkflowService: PaymentWorkflowService,
    private val tossBillingPaymentHandler: TossBillingPaymentHandler,
    @Value("\${payment.pending-timeout-seconds}")
    private val pendingTimeoutSeconds: Long,
    @Value("\${payment.reconciliation.max-window-seconds}")
    private val maxWindowSeconds: Long,
) {
    @Scheduled(fixedDelayString = "\${payment.reconciliation.interval-ms}")
    fun reconcileExpiredPendingTransactions() {
        val now = Instant.now()
        paymentTransactionService.findExpiredPending(now).forEach { transaction ->
            val result = tossBillingPaymentHandler.inquiry(transaction.moid)
            if (result is PaymentResult.Success) {
                paymentWorkflowService.completeByTransactionId(transaction.id!!, result.value.tid)
                return@forEach
            }
            if (result is PaymentResult.Failure) {
                paymentWorkflowService.failByTransactionId(transaction.id!!)
                return@forEach
            }
            if (transaction.createdAt!!.plusSeconds(maxWindowSeconds).isBefore(now)) {
                // TODO: 실패 확정 전에 Toss 취소(망취소) API를 호출해 혹시 승인된 결제를 되돌린다.
                paymentWorkflowService.failByTransactionId(transaction.id!!)
            } else {
                paymentTransactionService.extendExpiry(transaction.id!!, now.plusSeconds(pendingTimeoutSeconds))
            }
        }
    }
}
