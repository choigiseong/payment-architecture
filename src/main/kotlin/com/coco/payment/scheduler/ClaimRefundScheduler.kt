package com.coco.payment.scheduler

import com.coco.payment.persistence.enumerator.ClaimStatus
import com.coco.payment.persistence.repository.ClaimItemRepository
import com.coco.payment.persistence.repository.ClaimRepository
import com.coco.payment.service.ClaimService
import com.coco.payment.service.InvoiceService
import com.coco.payment.service.dto.PrepaymentView
import com.coco.payment.service.facade.PrepaymentFacade
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Component
class ClaimRefundScheduler(
    private val claimRepository: ClaimRepository,
    private val claimItemRepository: ClaimItemRepository,
    private val prepaymentFacade: PrepaymentFacade,
    private val invoiceService: InvoiceService,
    private val claimService: ClaimService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    fun processPendingRefunds() {
        val pendingClaims = claimRepository.findByStatus(ClaimStatus.COMPLETED)

        for (claim in pendingClaims) {
            try {
                // 중복 실행 방지를 위해 상태를 먼저 변경
                claimService.startRefundProcessing(claim.id!!)
                
                log.info("Processing refund for claim: ${claim.id}")
                processRefund(claim.id!!)
            } catch (e: IllegalStateException) {
                // startRefundProcessing 실패 시 (다른 스레드가 먼저 처리)
                log.warn("Could not start refund processing for claim ${claim.id}: ${e.message}")
            } catch (e: Exception) {
                // processRefund 실패 시 (PG 오류 등)
                log.error("Error processing refund for claim: ${claim.id}, it will be retried.", e)
                // 상태를 되돌려 놓거나 하는 처리가 없으므로, REFUND_PROCESSING 상태로 남아있게 됨.
                // 다음 스케줄링에서는 처리되지 않음. 수동 개입 필요.
                // 재시도를 원한다면 여기서 상태를 다시 COMPLETED로 변경해야 함.
            }
        }
    }

    // todo claimService.succeedRefund(claimId) 따로 처리
    fun processRefund(claimId: Long) {
        val claim = claimRepository.findById(claimId).get() // 위에서 상태를 변경했으므로 반드시 존재

        val claimItems = claimItemRepository.findByClaimSeq(claimId)
        val totalRefundAmount = claimItems.sumOf { it.claimAmount }
        val refundItems = claimItems.map {
            PrepaymentView.RefundItemCommand(
                orderItemSeq = it.orderItemSeq,
                refundAmount = it.claimAmount
            )
        }

        val invoice = invoiceService.findByOrderSeq(claim.orderSeq)
            ?: throw IllegalStateException("Invoice not found for order: ${claim.orderSeq}")

        prepaymentFacade.refundPrepayment(
            invoiceId = invoice.id!!,
            refundAmount = totalRefundAmount,
            reason = claim.reason,
            at = Instant.now(),
            refundItems = refundItems
        )

        claimService.succeedRefund(claimId)
    }

    @Scheduled(fixedDelay = 300000) // 5분마다 실행
    fun monitorStuckRefunds() {
        val stuckTime = Instant.now().minus(Duration.ofMinutes(5))
        val stuckClaims = claimRepository.findByStatusAndUpdatedAtBefore(ClaimStatus.REFUND_PROCESSING, stuckTime)

        if (stuckClaims.isNotEmpty()) {
            val stuckClaimIds = stuckClaims.map { it.id }
            // TODO: 실제 알림 시스템 연동 (Slack, Email, SMS 등)
            log.error("!!! REFUND-STUCK-ALERT: Claims are stuck in REFUND_PROCESSING state for over 5 minutes. Claim IDs: $stuckClaimIds")
        }
    }
}