package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.dto.PaymentResult
import com.coco.payment.handler.paymentgateway.toss.TossPaymentHandler
import com.coco.payment.handler.paymentgateway.dto.PgTransaction
import com.coco.payment.persistence.enumerator.DiscrepancyType
import com.coco.payment.persistence.enumerator.PaymentFailCode
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.enumerator.PgPaymentStatus
import com.coco.payment.persistence.model.PaymentTransaction
import com.coco.payment.support.Dates
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId

// 일일 대사: 전일 거래를 PG와 전수 대조한다. 돈을 돌려주는 방향만 자동으로 처분(취소)하고,
// 회수·조사 방향은 불일치로 적재해 사람에게 넘긴다. 적재까지가 이 잡의 일이다.
@Service
class DailyReconciliationService(
    private val paymentTransactionService: PaymentTransactionService,
    private val tossPaymentHandler: TossPaymentHandler,
    private val reconciliationDiscrepancyService: ReconciliationDiscrepancyService,
) {
    // TODO: 아래 세 단계 모두 건별로 예외를 삼키고 로그만 남긴다. 재처리는 다음 회차가 다시
    //  집으니 안전했지만, 대사는 창이 1일이고 결석 만회가 없어 빠진 거래가 영영 대사를 못 받는다.
    //  실패 건수를 세어 마지막에 잡을 실패시키면 재실행으로 만회할 수 있다(NOTES 3장 참고).
    @Scheduled(cron = "\${payment.reconciliation.daily-cron}", zone = Dates.ZONE_ID)
    fun reconcileYesterday() {
        val zone = ZoneId.of(Dates.ZONE_ID)
        val windowEnd = Dates.today().atStartOfDay()
        val windowStart = windowEnd.minusDays(1)
        val windowStartInstant = windowStart.atZone(zone).toInstant()
        val windowEndInstant = windowEnd.atZone(zone).toInstant()
        // 한 결제가 승인·취소 두 거래로 올 수 있어 moid로 접는다. 취소가 하나라도 있으면 취소로 본다.
        val tossByMoid = tossPaymentHandler.transactions(windowStart, windowEnd)
            .groupBy { it.orderId }
            .mapValues { (_, rows) -> rows.find { it.isCanceled } ?: rows.last() }

        for ((moid, pg) in tossByMoid) {
            // 건별로 격리한다. 하나가 실패해도 나머지가 이번 대사에서 빠지면 안 된다.
            try {
                reconcile(moid, pg)
            } catch (exception: Exception) {
                log.error("Failed to reconcile payment: $moid", exception)
            }
        }
        auditOurSuccesses(windowStartInstant, windowEndInstant, tossByMoid.keys)
        closeStuckPendings(windowStartInstant, windowEndInstant)
    }

    private fun reconcile(moid: String, pg: PgTransaction) {
        val ours = paymentTransactionService.findByMoid(moid)
        when {
            ours == null ->
                record(DiscrepancyType.ORPHAN, moid, null, pg)
            pg.isPaid && ours.isSuccess ->
                if (!ours.hasSameAmount(pg.amount)) record(DiscrepancyType.AMOUNT_MISMATCH, moid, ours, pg)
            pg.isPaid && ours.isFailed ->
                cancelLate(ours, pg)
            pg.isCanceled && ours.isSuccess ->
                record(DiscrepancyType.CANCELED_BUT_SUCCESS, moid, ours, pg)
        }
    }

    // 우리는 실패로 종료했는데 돈은 나간 거래 — 이 잡의 존재 이유. 되살리지 않고 취소한다.
    // 주문이 살아 있어 사용자가 이미 재결제했을 수 있고, 복원하면 한 주문에 결제가 둘이 된다.
    private fun cancelLate(ours: PaymentTransaction, pg: PgTransaction) {
        val result = tossPaymentHandler.cancel(pg.tid, RECON_CANCEL_REASON)
        if (result is PaymentResult.Success) {
            paymentTransactionService.overwriteFailure(ours.id!!, pg.tid, PaymentFailCode.RECON_CANCEL, "대사가 뒤늦게 확인된 결제를 취소했습니다.")
        } else {
            record(DiscrepancyType.CANCEL_FAILED, ours.moid, ours, pg, detail = "취소 실패")
        }
    }

    // PG 목록에 아예 없는 우리 성공 거래. 승인 시각 기준으로 잘라 PG 목록과 축을 맞췄지만,
    // PG의 거래 시각과 미세하게 어긋날 수 있어 개별 조회로 한 번 더 확인한다.
    private fun auditOurSuccesses(windowStart: Instant, windowEnd: Instant, tossMoids: Set<String>) {
        val successes = paymentTransactionService.findSuccessesApprovedBetween(windowStart, windowEnd)
        for (transaction in successes.filter { it.moid !in tossMoids }) {
            try {
                when (val result = tossPaymentHandler.inquiry(transaction.moid)) {
                    is PaymentResult.Success -> Unit
                    is PaymentResult.Failure ->
                        record(DiscrepancyType.CANCELED_BUT_SUCCESS, transaction.moid, transaction, null, PgPaymentStatus.CANCELED, result.error.message)
                    is PaymentResult.Unknown ->
                        record(DiscrepancyType.MISSING_AT_PG, transaction.moid, transaction, null, detail = result.error.message)
                }
            } catch (exception: Exception) {
                log.error("Failed to audit payment: ${transaction.moid}", exception)
            }
        }
    }

    // 어제 만들어졌는데 아직 안 끝난 거래를 종결한다. 재처리가 5분 룰로 끝냈어야 하는 것들이라
    // 전부 기한을 한참 넘겼고, 이 단계가 끝나면 어제 거래는 종착 상태만 남는다.
    private fun closeStuckPendings(windowStart: Instant, windowEnd: Instant) {
        for (transaction in paymentTransactionService.findPendingsCreatedBetween(windowStart, windowEnd)) {
            try {
                closeStuckPending(transaction)
            } catch (exception: Exception) {
                log.error("Failed to close stuck payment: ${transaction.id}", exception)
            }
        }
    }

    private fun closeStuckPending(transaction: PaymentTransaction) {
        when (val result = tossPaymentHandler.inquiry(transaction.moid)) {
            is PaymentResult.Success -> {
                val cancel = tossPaymentHandler.cancel(result.value.tid, RECON_CANCEL_REASON)
                if (cancel is PaymentResult.Success) {
                    paymentTransactionService.fail(transaction.id!!, PaymentFailCode.RECON_CANCEL, "대사가 뒤늦게 확인된 결제를 취소했습니다.")
                } else {
                    paymentTransactionService.fail(transaction.id!!, PaymentFailCode.CANCEL_FAILED, "취소하지 못한 채 종결했습니다. 확인이 필요합니다.")
                    record(DiscrepancyType.CANCEL_FAILED, transaction.moid, transaction, null, PgPaymentStatus.PAID, "취소 실패")
                }
            }
            is PaymentResult.Failure ->
                paymentTransactionService.fail(transaction.id!!, PaymentFailCode.PG_CANCELED, result.error.reason)
            is PaymentResult.Unknown ->
                paymentTransactionService.fail(transaction.id!!, PaymentFailCode.NOT_CONFIRMED, "기한 안에 결제를 확인하지 못했습니다.")
        }
    }

    // 감지 시점의 양쪽 상태·금액을 얼려서 OPEN으로 넣는다. 중복 검사는 없다 — 재감지되면 또 쌓이고 정리는 관리자 몫.
    private fun record(
        type: DiscrepancyType,
        moid: String,
        ours: PaymentTransaction?,
        pg: PgTransaction?,
        pgStatus: PgPaymentStatus? = pg?.status,
        detail: String? = null,
    ) {
        val fullDetail = listOfNotNull(detail, pg?.let { "PG 상태: ${it.rawStatus}" })
            .joinToString(" / ")
            .ifBlank { null }
        reconciliationDiscrepancyService.create(
            PaymentSystem.TOSS, type, moid,
            ours?.status, pgStatus, ours?.amount, pg?.amount, fullDetail,
        )
    }

    companion object {
        private const val RECON_CANCEL_REASON = "대사에서 뒤늦게 확인된 결제 취소"
        private val log = LoggerFactory.getLogger(DailyReconciliationService::class.java)
    }
}
