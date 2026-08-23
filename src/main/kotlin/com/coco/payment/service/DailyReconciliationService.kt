package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.toss.TossPaymentHandler
import com.coco.payment.handler.paymentgateway.dto.PgTransaction
import com.coco.payment.persistence.enumerator.DiscrepancyType
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.PaymentTransaction
import com.coco.payment.support.Dates
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

// 일일 대사: 전일 거래를 PG와 전수 대조하고, 어긋난 것을 불일치로 적재해 사람에게 넘긴다.
// 적재까지가 이 잡의 일이다 — PG를 쓰기로 건드리지 않고, 거래 상태도 바꾸지 않는다.
@Service
class DailyReconciliationService(
    private val paymentTransactionService: PaymentTransactionService,
    private val tossPaymentHandler: TossPaymentHandler,
    private val reconciliationDiscrepancyService: ReconciliationDiscrepancyService,
) {
    // TODO: reconcile()이 건별로 예외를 삼키고 로그만 남긴다. 재처리는 다음 회차가 다시 집으니
    //  안전했지만, 대사는 창이 1일이고 결석 만회가 없어 빠진 거래가 영영 대사를 못 받는다.
    //  실패 건수를 세어 마지막에 잡을 실패시키면 재실행으로 만회할 수 있다(NOTES 3장 참고).
    @Scheduled(cron = "\${payment.reconciliation.daily-cron}", zone = Dates.ZONE_ID)
    fun reconcileYesterday() {
        val windowEnd = Dates.today().atStartOfDay()
        val windowStart = windowEnd.minusDays(1)
        // 한 결제가 승인·취소 두 거래로 올 수 있어 moid로 접는다. 취소가 하나라도 있으면 취소로 본다.
        val tossByMoid = tossPaymentHandler.transactions(windowStart, windowEnd)
            .groupBy { it.orderId }
            .mapValues { (_, rows) -> rows.find { it.isCanceled } ?: rows.find { it.isPaid } ?: rows.last() }

        for ((moid, pg) in tossByMoid) {
            try {
                reconcile(moid, pg)
            } catch (exception: Exception) {
                log.error("Failed to reconcile payment: $moid", exception)
            }
        }
        recordFromOurSide(windowStart.atZone(Dates.SEOUL).toInstant(), windowEnd.atZone(Dates.SEOUL).toInstant(), tossByMoid.keys)
    }

    private fun reconcile(moid: String, pg: PgTransaction) {
        val ours = paymentTransactionService.findByMoid(moid)
        when {
            ours == null ->
                record(DiscrepancyType.ORPHAN, moid, null, pg)
            // 아직 안 끝난 거래는 아래 훑기가 전량 적재한다. 끝낼지 말지는 대사가 정할 일이 아니다.
            ours.isPending -> Unit
            pg.isPaid && ours.isSuccess ->
                if (!ours.hasSameAmount(pg.amount)) record(DiscrepancyType.AMOUNT_MISMATCH, moid, ours, pg)
            // 되살리지도, 취소하지도 않는다. 사용자가 이미 재결제했을 수 있어 사람이 보고 정해야 한다.
            pg.isPaid && ours.isFailed ->
                record(DiscrepancyType.PAID_BUT_FAILED, moid, ours, pg)
            pg.isCanceled && ours.isSuccess ->
                record(DiscrepancyType.CANCELED_BUT_SUCCESS, moid, ours, pg)
            // 정합 — 우리가 취소했거나 취소를 확인하고 종결한 거래.
            pg.isCanceled && ours.isFailed -> Unit
            // PG 상태가 판정 어휘 밖(UNKNOWN). 전수 대조에서 판정 불가는 성공이 아니라 불일치다.
            else ->
                record(DiscrepancyType.UNRESOLVED, moid, ours, pg)
        }
    }

    // 우리 쪽 목록을 훑는다. PG에 없는 성공은 개별 조회로 재확인하지 않는다 — 우리 approved_at도
    // PG가 준 값이라 두 목록의 축이 같고, 어긋난다면 그게 곧 사람이 봐야 할 불일치다.
    // 미결은 승인 시각이 없어 생성 시각으로 자르고, PG에 있든 없든 끝나지 않은 것은 다 올린다.
    private fun recordFromOurSide(windowStart: Instant, windowEnd: Instant, tossMoids: Set<String>) {
        for (transaction in paymentTransactionService.findPendingsCreatedBetween(windowStart, windowEnd)) {
            record(DiscrepancyType.STUCK_PENDING, transaction.moid, transaction, null)
        }
        for (transaction in paymentTransactionService.findSuccessesApprovedBetween(windowStart, windowEnd)) {
            if (transaction.moid !in tossMoids) record(DiscrepancyType.MISSING_AT_PG, transaction.moid, transaction, null)
        }
    }

    // 감지 시점의 양쪽 상태·금액을 얼려서 OPEN으로 넣는다. 중복 검사는 없다 — 정리는 관리자 몫.
    private fun record(type: DiscrepancyType, moid: String, ours: PaymentTransaction?, pg: PgTransaction?) {
        reconciliationDiscrepancyService.create(
            PaymentSystem.TOSS, type, moid,
            ours?.status, pg?.status, ours?.amount, pg?.amount, if (pg != null) "PG 상태: ${pg.rawStatus}" else null,
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(DailyReconciliationService::class.java)
    }
}
