package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.toss.TossPaymentHandler
import com.coco.payment.handler.paymentgateway.dto.PgTransaction
import com.coco.payment.persistence.enumerator.DiscrepancyType
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.PaymentCancel
import com.coco.payment.persistence.model.PaymentTransaction
import com.coco.payment.support.Dates
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

// 일일 대사: 전일 거래를 PG와 전수 대조하고, 어긋난 것을 불일치로 적재해 사람에게 넘긴다.
// 적재까지가 이 잡의 일이다 — PG를 쓰기로 건드리지 않고, 거래 상태도 바꾸지 않는다.
// 축이 둘이다. 승인 거래는 payment_transaction과, 취소 거래는 payment_cancel과 대조한다.
@Service
class DailyReconciliationService(
    private val paymentTransactionService: PaymentTransactionService,
    private val paymentCancelService: PaymentCancelService,
    private val tossPaymentHandler: TossPaymentHandler,
    private val reconciliationDiscrepancyService: ReconciliationDiscrepancyService,
) {
    // TODO: reconcile이 건별로 예외를 삼키고 로그만 남긴다. 재처리는 다음 회차가 다시 집으니
    //  안전했지만, 대사는 창이 1일이고 결석 만회가 없어 빠진 거래가 영영 대사를 못 받는다.
    //  실패 건수를 세어 마지막에 잡을 실패시키면 재실행으로 만회할 수 있다(NOTES 참고).
    @Scheduled(cron = "\${payment.reconciliation.daily-cron}", zone = Dates.ZONE_ID)
    fun reconcileYesterday() {
        val windowEnd = Dates.today().atStartOfDay()
        val windowStart = windowEnd.minusDays(1)
        val pgRows = tossPaymentHandler.transactions(windowStart, windowEnd)

        for (pg in pgRows) {
            try {
                reconcile(pg)
            } catch (exception: Exception) {
                log.error("Failed to reconcile pg transaction: ${pg.transactionKey}", exception)
            }
        }
        recordFromOurSide(Dates.seoulToInstant(windowStart), Dates.seoulToInstant(windowEnd), pgRows)
    }

    private fun reconcile(pg: PgTransaction) {
        when {
            pg.isPaid -> reconcilePayment(pg)
            pg.isCanceled -> reconcileCancel(pg)
            pg.isNotCompleted -> reconcileNotCompleted(pg)
            // PG 상태가 판정 어휘 밖(UNKNOWN). 전수 대조에서 판정 불가는 성공이 아니라 불일치다.
            else -> record(DiscrepancyType.UNRESOLVED, pg.orderId, paymentTransactionService.findByMoid(pg.orderId), pg)
        }
    }

    private fun reconcilePayment(pg: PgTransaction) {
        val ours = paymentTransactionService.findByMoid(pg.orderId)
        when {
            ours == null ->
                record(DiscrepancyType.ORPHAN, pg.orderId, null, pg)
            ours.isPending ->
                record(DiscrepancyType.STUCK_PENDING, pg.orderId, ours, pg)
            ours.isSuccess ->
                if (!ours.hasSameAmount(pg.amount)) record(DiscrepancyType.AMOUNT_MISMATCH, pg.orderId, ours, pg)
            // 정합 — 성공이었지만 취소되었다. 취소가 실제 끝났는지는 취소 축이 본다.
            ours.isCanceled && ours.approvedAt != null -> Unit
            ours.isFailed ->
                record(DiscrepancyType.PAID_BUT_FAILED, pg.orderId, ours, pg)
            else ->
                record(DiscrepancyType.UNRESOLVED, pg.orderId, ours, pg)
        }
    }

    private fun reconcileCancel(pg: PgTransaction) {
        if (paymentCancelService.findByTransactionKey(pg.transactionKey) != null) return

        val ours = paymentTransactionService.findByMoid(pg.orderId)
        // 키를 아직 못 받은 우리 취소다(응답 유실). 아래 recordFromOurSide가 STUCK_CANCEL로 적재한다.
        if (ours != null && paymentCancelService.findRequestedByTransactionSeq(ours.id!!) != null) return

        record(DiscrepancyType.UNKNOWN_CANCEL, pg.orderId, ours, pg)
    }

    private fun reconcileNotCompleted(pg: PgTransaction) {
        val ours = paymentTransactionService.findByMoid(pg.orderId)
        when {
            ours == null ->
                record(DiscrepancyType.ORPHAN, pg.orderId, null, pg)
            ours.isPending ->
                record(DiscrepancyType.STUCK_PENDING, pg.orderId, ours, pg)
            ours.isSuccess ->
                record(DiscrepancyType.NOT_COMPLETED_BUT_SUCCESS, pg.orderId, ours, pg)
            ours.isFailed -> Unit
            else ->
                record(DiscrepancyType.UNRESOLVED, pg.orderId, ours, pg)
        }
    }

    private fun recordFromOurSide(windowStart: Instant, windowEnd: Instant, pgRows: List<PgTransaction>) {
        val pgMoids = pgRows.mapTo(mutableSetOf()) { it.orderId }
        val pgPaidMoids = pgRows.filter { it.isPaid }.mapTo(mutableSetOf()) { it.orderId }
        val pgCancelKeys = pgRows.filter { it.isCanceled }.mapTo(mutableSetOf()) { it.transactionKey }

        for (transaction in paymentTransactionService.findPendingsCreatedBetween(windowStart, windowEnd)) {
            if (transaction.moid !in pgMoids) record(DiscrepancyType.STUCK_PENDING, transaction.moid, transaction, null)
        }
        for (transaction in paymentTransactionService.findSuccessesApprovedBetween(windowStart, windowEnd)) {
            if (transaction.moid !in pgPaidMoids) record(DiscrepancyType.MISSING_AT_PG, transaction.moid, transaction, null)
        }
        for (cancel in paymentCancelService.findRequestedCreatedBetween(windowStart, windowEnd)) {
            recordCancel(DiscrepancyType.STUCK_CANCEL, cancel, cancel.lastError)
        }
        for (cancel in paymentCancelService.findDoneCanceledBetween(windowStart, windowEnd)) {
            if (cancel.transactionKey !in pgCancelKeys) {
                recordCancel(DiscrepancyType.CANCEL_MISSING_AT_PG, cancel, cancel.transactionKey)
            }
        }
    }

    private fun recordCancel(type: DiscrepancyType, cancel: PaymentCancel, detail: String?) {
        val transaction = paymentTransactionService.findById(cancel.paymentTransactionSeq)
        record(type, transaction?.moid ?: "payment-cancel-${cancel.id}", transaction, null, detail)
    }

    // 감지 시점의 양쪽 상태·금액을 얼려서 OPEN으로 넣는다. 중복 검사는 없다 — 정리는 관리자 몫.
    private fun record(type: DiscrepancyType, moid: String, ours: PaymentTransaction?, pg: PgTransaction?, detail: String? = null) {
        val pgDetail = if (pg != null) "PG 상태: ${pg.rawStatus}" else null
        reconciliationDiscrepancyService.create(
            PaymentSystem.TOSS, type, moid,
            ours?.status, pg?.status, ours?.amount, pg?.amount, detail ?: pgDetail,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyReconciliationService::class.java)
    }
}
