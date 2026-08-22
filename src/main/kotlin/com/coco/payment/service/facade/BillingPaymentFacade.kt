package com.coco.payment.service.facade

import com.coco.payment.handler.paymentgateway.toss.TossPaymentHandler
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentCommand
import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus
import com.coco.payment.service.OrderService
import com.coco.payment.service.PaymentWorkflowService
import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.service.dto.BillingPaymentResult
import com.coco.payment.service.dto.PrepareBillingPaymentResult
import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import org.springframework.stereotype.Service

@Service
class BillingPaymentFacade(
    private val orderService: OrderService,
    private val paymentWorkflowService: PaymentWorkflowService,
    private val tossPaymentHandler: TossPaymentHandler,
) {
    fun pay(command: BillingPaymentCommand): BillingPaymentResult {
        val existingTransaction = paymentWorkflowService.findByPaymentKey(command.paymentKey)
        if (existingTransaction != null) {
            val order = orderService.findById(existingTransaction.orderSeq)
                ?: throw IllegalStateException("Order not found for payment transaction: ${existingTransaction.id}")
            return BillingPaymentResult(
                order.orderKey,
                existingTransaction.paymentKey,
                order.status,
                existingTransaction.status,
                order.deliveryDate,
                existingTransaction.tid,
                existingTransaction.failCode,
                existingTransaction.failMessage,
            )
        }

        val prepared = paymentWorkflowService.prepare(command)
        if (prepared is PrepareBillingPaymentResult.AlreadyPending) {
            val order = orderService.findById(prepared.orderId)
                ?: throw IllegalStateException("Order not found for payment transaction with paymentKey: ${prepared.paymentKey}")
            return BillingPaymentResult(order.orderKey, prepared.paymentKey, order.status, prepared.status, order.deliveryDate, prepared.tid, null, null)
        }
        val result = prepared as PrepareBillingPaymentResult.Ready

        // TODO: 여기서 Toss 승인을 동기로 기다린다(요청 스레드 점유). 전환 이유가 둘이다.
        //  (1) 부하 — 스레드 풀 고갈이 문제가 되는 규모. 지금은 아니다.
        //  (2) 가용성 — Toss가 죽으면 스레드가 물려 주문 접수 자체가 막힌다. 이쪽은 규모와
        //      무관한 장애 격리 문제라 "규모가 오면"으로 미룰 수 있는 성격이 아니다.
        //  전환은 네이버식으로 한다 — 접수(prepare)까지만 하고 즉시 응답,
        //  승인은 워커가 비동기 처리, 클라이언트는 처음부터 결과 페이지 폴링으로 확정.
        //  결과 페이지/지수 백오프 폴링/PENDING 재처리 스케줄러는 그대로 재사용 가능하다.
        val approveResult = tossPaymentHandler.approve(
            TossBillingPaymentCommand(result.billingKey, result.customerKey, result.moid, result.orderName, result.amount)
        )

        return when (approveResult) {
            is PaymentResult.Success -> {
                // TODO: complete()가 실패하면 예외가 그대로 올라가 500이 나가고, 클라이언트는
                //  "다시 시도해 주세요"를 띄운다. 승인은 이미 성공해 돈이 빠져나간 뒤라 최악의 안내다.
                //  승인 전 실패(돈 안 나감, 재시도가 맞음)와 승인 후 실패(돈 나감, 확인으로 보내야 함)를
                //  구분해야 한다. 후자는 Unknown과 같이 PENDING 응답으로 결과 페이지에 보내는 편이 맞다.
                //  불확실한 Unknown은 폴링시키면서 성공을 확인한 이 경로를 더 나쁘게 다루고 있다.
                //  받아둔 tid도 롤백과 함께 버려진다. 스케줄러가 inquiry로 되찾지만, 그때 Toss가 계속
                //  안 잡히면 30분 뒤 실패로 확정돼 Toss는 성공인데 우리 DB만 FAILED가 된다.
                paymentWorkflowService.complete(result, approveResult.value)
                BillingPaymentResult(result.orderKey, result.paymentKey, OrderStatus.PAID, PaymentTransactionStatus.SUCCESS, command.deliveryDate, approveResult.value.tid, null, null)
            }
            is PaymentResult.Failure -> {
                paymentWorkflowService.fail(result, approveResult.error.code, approveResult.error.message)
                // 이번 시도만 실패했을 뿐 주문은 아직 미결제 상태다(같은 orderKey로 재시도 가능).
                BillingPaymentResult(result.orderKey, result.paymentKey, OrderStatus.PENDING_PAYMENT, PaymentTransactionStatus.FAILED, command.deliveryDate, null, approveResult.error.code, approveResult.error.message)
            }
            is PaymentResult.Unknown ->
                BillingPaymentResult(result.orderKey, result.paymentKey, OrderStatus.PENDING_PAYMENT, PaymentTransactionStatus.PENDING, command.deliveryDate, null, approveResult.error.code, approveResult.error.message)
        }
    }

    fun poll(paymentKey: String): BillingPaymentResult? {
        val transaction = paymentWorkflowService.findByPaymentKey(paymentKey) ?: return null
        val order = orderService.findById(transaction.orderSeq) ?: return null
        return BillingPaymentResult(order.orderKey, transaction.paymentKey, order.status, transaction.status, order.deliveryDate, transaction.tid, transaction.failCode, transaction.failMessage)
    }
}
