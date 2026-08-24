package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import com.coco.payment.handler.paymentgateway.toss.TossPaymentHandler
import com.coco.payment.persistence.enumerator.PaymentFailCode
import com.coco.payment.persistence.model.PaymentTransaction
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PaymentRecheckService(
    private val paymentTransactionService: PaymentTransactionService,
    private val paymentWorkflowService: PaymentWorkflowService,
    private val tossPaymentHandler: TossPaymentHandler,
) {
    @Scheduled(fixedDelayString = "\${payment.recheck.interval-ms}")
    fun recheckPendingTransactions() {
        for (transaction in paymentTransactionService.findPendingDueForCheck(PaymentTransaction.approveDoneBefore(Instant.now()))) {
            try {
                // 조회가 순차라 회차가 210초를 넘길 수 있다. 회차 시작 시각으로 판정하면 뒤쪽 거래가
                // 기한을 넘기고도 "기한 안"이 되어, 취소해야 할 것을 확정해 버린다.
                recheck(transaction, Instant.now())
            } catch (exception: Exception) {
                log.error("Failed to recheck payment transaction: ${transaction.id}", exception)
            }
        }
    }

    // 거래는 생성 후 정해진 기한 안에 성공이나 실패로 끝나야 한다. 기한을 넘기면 배송 마감을
    // 지킬 수 없으므로, 승인이 성공했더라도 되돌려 없던 일로 만든다.
    private fun recheck(transaction: PaymentTransaction, now: Instant) {
        val expired = transaction.isExpired(now)
        when (val result = tossPaymentHandler.inquiry(transaction.moid)) {
            is PaymentResult.Success ->
                if (expired) {
                    netCancel(transaction, result.value.tid)
                } else {
                    paymentWorkflowService.completeByTransactionId(transaction.id!!, result.value.tid, result.value.approvedAt)
                }
            is PaymentResult.Failure ->
                paymentWorkflowService.failByTransactionId(transaction.id!!, PaymentFailCode.PG_CANCELED, result.error.reason)
            is PaymentResult.Unknown ->
                // 확정하지 못했다(결제 내역이 없는 경우 포함). 기한 안이면 다음 회차가 다시 집고,
                // 넘겼으면 승인이 도달한 적 없는 것으로 보고 종료한다. 실제로 성공한 거래였다면
                // 다음날 대사가 PAID_BUT_FAILED로 적재한다.
                if (expired) {
                    paymentWorkflowService.failByTransactionId(transaction.id!!, PaymentFailCode.NOT_CONFIRMED, "기한 안에 결제를 확인하지 못했습니다.")
                }
        }
    }

    // 취소 실패는 PENDING으로 남겨 다음 회차가 다시 시도하게 한다. 이미 취소된 결제라면 조회가
    // CANCELED를 돌려줘 Failure 가지로 끝나므로 여기 오지 않는다. 그 외 사유(NOT_CANCELABLE_* 등)로
    // 계속 실패하면 미결로 남는다 — 끊는 기준은 두지 않았고, 대사도 개입하지 않는다.
    private fun netCancel(transaction: PaymentTransaction, tid: String) {
        val result = tossPaymentHandler.cancel(tid, "결제 확정 기한 초과")
        if (result is PaymentResult.Success) {
            paymentWorkflowService.failByTransactionId(transaction.id!!, PaymentFailCode.NET_CANCEL, "확정 기한을 넘겨 결제를 취소했습니다.", tid)
            return
        }
        log.error("Failed to cancel payment transaction: ${transaction.id}, tid: $tid, reason: ${result.errorOrNull?.reason}")
    }

    companion object {
        private val log = LoggerFactory.getLogger(PaymentRecheckService::class.java)
    }
}
