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
    private val invoiceService: InvoiceService
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

    // 검수 완료 (환불 대기 상태로 변경)
    @Transactional
    fun completeClaim(claimSeq: Long) {
        val affected = claimRepository.updateStatus(
            claimSeq,
            setOf(ClaimStatus.REQUESTED, ClaimStatus.INSPECTING),
            ClaimStatus.COMPLETED
        )
        if (affected != 1) {
            val claim = claimRepository.findById(claimSeq).get()
            throw IllegalStateException("Invalid claim status for completion: ${claim.status}")
        }
    }

    @Transactional
    fun startRefundProcessing(claimId: Long) {
        val affected = claimRepository.updateStatus(
            claimId,
            setOf(ClaimStatus.COMPLETED),
            ClaimStatus.REFUND_PROCESSING
        )
        if (affected != 1) {
            throw IllegalStateException("Claim $claimId is already being processed or in an invalid state.")
        }
    }

    @Transactional
    fun succeedRefund(claimId: Long) {
        val affected = claimRepository.updateStatus(
            claimId,
            setOf(ClaimStatus.REFUND_PROCESSING),
            ClaimStatus.REFUNDED
        )
        if (affected != 1) {
            throw IllegalStateException("Invalid claim status for refund success: $claimId")
        }
    }

    @Transactional
    fun failRefund(claimId: Long) {
        val affected = claimRepository.updateStatus(
            claimId,
            setOf(ClaimStatus.REFUND_PROCESSING),
            ClaimStatus.REFUND_FAILED
        )
        if (affected != 1) {
            throw IllegalStateException("Invalid claim status for refund failure: $claimId")
        }
    }

    data class ClaimItemRequest(
        val orderItemSeq: Long,
        val quantity: Int,
        val claimAmount: Long
    )
}