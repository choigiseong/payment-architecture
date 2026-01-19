package com.coco.payment.service.event

import com.coco.payment.service.ClaimService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ClaimRefundEventListener(
    private val claimService: ClaimService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleRefundSuccessEvent(event: RefundSuccessEvent) {
        try {
            log.info("Handling RefundSuccessEvent for claim: ${event.claimId}")
            claimService.succeedRefund(event.claimId)
        } catch (e: Exception) {
            log.error("Failed to update claim status for claim: ${event.claimId}. It will be recovered by scheduler.", e)
            // 여기서 예외가 발생해도 이미 환불 트랜잭션은 커밋되었으므로 환불은 성공한 상태임.
            // 스케줄러가 나중에 상태를 보정해줄 것임.
        }
    }
}
