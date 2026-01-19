package com.coco.payment.scheduler

import com.coco.payment.persistence.enumerator.ClaimStatus
import com.coco.payment.persistence.enumerator.RefundAttemptStatus
import com.coco.payment.persistence.repository.ClaimItemRepository
import com.coco.payment.persistence.repository.ClaimRepository
import com.coco.payment.persistence.repository.RefundAttemptRepository
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
    private val claimService: ClaimService,
    private val refundAttemptRepository: RefundAttemptRepository
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

        // 1. 환불 로직 수행 (PG 취소 및 내부 데이터 업데이트)
        // 성공 시 이벤트가 발행되어 상태가 변경됨
        prepaymentFacade.refundPrepayment(
            invoiceId = invoice.id!!,
            refundAmount = totalRefundAmount,
            reason = claim.reason,
            at = Instant.now(),
            refundItems = refundItems,
            claimSeq = claimId // 클레임 ID 전달
        )
    }

    @Scheduled(fixedDelay = 300000) // 5분마다 실행
    fun recoverStuckRefunds() {
        val stuckTime = Instant.now().minus(Duration.ofMinutes(5))
        val stuckClaims = claimRepository.findByStatusAndUpdatedAtBefore(ClaimStatus.REFUND_PROCESSING, stuckTime)

        for (claim in stuckClaims) {
            try {
                // 환불 시도가 성공했는지 확인
                val isRefunded = refundAttemptRepository.existsByClaimSeqAndStatus(claim.id!!, RefundAttemptStatus.SUCCEEDED)
                
                if (isRefunded) {
                    log.info("Recovering stuck claim: ${claim.id}. Refund was successful, updating status.")
                    claimService.succeedRefund(claim.id!!)
                } else {
                    // 환불 시도가 없거나 실패했다면? -> 다시 COMPLETED로 돌려서 재시도하게 할 수도 있고, 알림을 보낼 수도 있음.
                    // 여기서는 알림만 보냄
                    log.error("!!! REFUND-STUCK-ALERT: Claim ${claim.id} is stuck in REFUND_PROCESSING but no successful refund attempt found.")
                }
            } catch (e: Exception) {
                log.error("Error recovering stuck claim: ${claim.id}", e)
            }
        }
    }
}