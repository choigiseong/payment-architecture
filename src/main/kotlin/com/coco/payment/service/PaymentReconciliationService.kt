package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import com.coco.payment.handler.paymentgateway.toss.TossPaymentHandler
import com.coco.payment.persistence.model.PaymentTransaction
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PaymentReconciliationService(
    private val paymentTransactionService: PaymentTransactionService,
    private val paymentWorkflowService: PaymentWorkflowService,
    private val tossPaymentHandler: TossPaymentHandler,
) {
    @Scheduled(fixedDelayString = "\${payment.reconciliation.interval-ms}")
    fun reconcilePendingTransactions() {
        val now = Instant.now()
        for (transaction in paymentTransactionService.findPendingDueForCheck(PaymentTransaction.approveDoneBefore(now))) {
            // 건별로 격리한다. 하나가 실패해도 나머지가 이번 회차에서 빠지면 안 된다.
            try {
                reconcile(transaction, now)
            } catch (exception: Exception) {
                log.error("Failed to reconcile payment transaction: ${transaction.id}", exception)
            }
        }
    }

    // 거래는 생성 후 정해진 기한 안에 성공이나 실패로 끝나야 한다. 기한을 넘기면 배송 마감을
    // 지킬 수 없으므로, 승인이 성공했더라도 되돌려 없던 일로 만든다.
    private fun reconcile(transaction: PaymentTransaction, now: Instant) {
        val expired = transaction.isExpired(now)
        when (val result = tossPaymentHandler.inquiry(transaction.moid)) {
            is PaymentResult.Success ->
                if (expired) {
                    netCancel(transaction, result.value.tid)
                } else {
                    paymentWorkflowService.completeByTransactionId(transaction.id!!, result.value.tid)
                }
            is PaymentResult.Failure ->
                paymentWorkflowService.failByTransactionId(transaction.id!!, result.error.code, result.error.message)
            is PaymentResult.Unknown ->
                // 조회로 확정하지 못했다(결제 내역이 없는 경우 포함). 기한 안이면 다음 회차에 다시
                // 걸리므로 아무것도 하지 않고, 넘겼으면 승인이 도달한 적 없는 것으로 보고 종료한다.
                // TODO: 이 종료는 눈감고 내리는 결론이다. Toss가 죽어 조회가 계속 타임아웃이면 승인이
                //  성공했는데도 FAILED로 끝나 돈이 유실된다. 하루 뒤 재조회하는 일일 대사가 받아줘야
                //  정당해진다(NOTES 3장 「거래대사」, 별도 브랜치).
                if (expired) {
                    paymentWorkflowService.failByTransactionId(transaction.id!!, NOT_CONFIRMED_CODE, "기한 안에 결제를 확인하지 못했습니다.")
                }
        }
    }

    // 이미 취소된 결제는 조회가 CANCELED를 돌려줘 위의 Failure 가지로 끝나므로 여기 오지 않는다.
    // 그래서 취소 실패는 PENDING으로 남겨 다음 회차에 다시 시도해도 무한히 반복되지 않는다.
    private fun netCancel(transaction: PaymentTransaction, tid: String) {
        val result = tossPaymentHandler.cancel(tid, "결제 확정 기한 초과")
        if (result is PaymentResult.Success) {
            paymentWorkflowService.failByTransactionId(transaction.id!!, NET_CANCEL_CODE, "확정 기한을 넘겨 결제를 취소했습니다.")
            return
        }
        log.error("Failed to cancel payment transaction: ${transaction.id}, tid: $tid")
    }

    companion object {
        private const val NOT_CONFIRMED_CODE = "NOT_CONFIRMED"
        private const val NET_CANCEL_CODE = "NET_CANCEL"
        private val log = LoggerFactory.getLogger(PaymentReconciliationService::class.java)
    }
}
