package com.coco.payment.service

import com.coco.payment.persistence.enumerator.ClaimStatus
import com.coco.payment.persistence.model.Claim
import com.coco.payment.persistence.model.ClaimItem
import com.coco.payment.persistence.repository.ClaimItemRepository
import com.coco.payment.persistence.repository.ClaimRepository
import com.coco.payment.service.dto.PrepaymentView
import com.coco.payment.service.facade.PrepaymentFacade
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ClaimService(
    private val claimRepository: ClaimRepository,
    private val claimItemRepository: ClaimItemRepository,
    private val prepaymentFacade: PrepaymentFacade,
    private val invoiceService: InvoiceService // Invoice 조회를 위해 추가
) {

    @Transactional
    fun requestClaim(
        customerSeq: Long,
        orderSeq: Long,
        reason: String,
        items: List<ClaimItemRequest>
    ): Long {
        val claim = claimRepository.save(
            Claim(
                customerSeq = customerSeq,
                orderSeq = orderSeq,
                reason = reason,
                status = ClaimStatus.REQUESTED
            )
        )

        val claimItems = items.map {
            ClaimItem(
                claimSeq = claim.id!!,
                orderItemSeq = it.orderItemSeq,
                quantity = it.quantity,
                claimAmount = it.claimAmount
            )
        }
        claimItemRepository.saveAll(claimItems)

        return claim.id!!
    }

    // 검수 완료 및 환불 진행
    @Transactional
    fun completeClaim(claimSeq: Long) {
        val claim = claimRepository.findById(claimSeq)
            .orElseThrow { IllegalArgumentException("Claim not found") }

        // 1. 상태 변경 (반품 확정)
        val affected = claimRepository.updateStatus(
            claim.id!!,
            setOf(ClaimStatus.REQUESTED, ClaimStatus.INSPECTING),
            ClaimStatus.COMPLETED
        )
        if (affected != 1) {
            throw IllegalStateException("Invalid claim status for completion: ${claim.status}")
        }

        // 2. 환불 요청 데이터 준비
        val claimItems = claimItemRepository.findByClaimSeq(claimSeq)
        val totalRefundAmount = claimItems.sumOf { it.claimAmount }
        val refundItems = claimItems.map {
            PrepaymentView.RefundItemCommand(
                orderItemSeq = it.orderItemSeq,
                refundAmount = it.claimAmount
            )
        }

        // 3. 환불 실행 (PrepaymentFacade 호출)
        // Claim의 orderSeq를 사용하여 관련된 Invoice를 찾아야 함.
        // 여기서는 orderSeq와 1:1 매칭되는 Invoice가 있다고 가정.
        val invoice = invoiceService.findByOrderSeq(claim.orderSeq)
            ?: throw IllegalStateException("Invoice not found for order: ${claim.orderSeq}")

        // 주의: 여기서 예외 발생 시 Claim 상태 변경도 롤백됨 (Transactional)
        prepaymentFacade.refundPrepayment(
            invoiceId = invoice.id!!,
            refundAmount = totalRefundAmount,
            reason = claim.reason,
            at = Instant.now(),
            refundItems = refundItems
        )

        // 4. 환불 완료 상태로 변경 (선택 사항, 환불 로직이 동기적으로 성공했다고 가정)
        claimRepository.updateStatus(
            claim.id!!,
            setOf(ClaimStatus.COMPLETED),
            ClaimStatus.REFUNDED
        )
    }

    data class ClaimItemRequest(
        val orderItemSeq: Long,
        val quantity: Int,
        val claimAmount: Long
    )
}