package com.coco.payment.persistence.model

import java.time.Instant
import org.apache.ibatis.type.Alias
import com.coco.payment.persistence.enumerator.PaymentFailCode
import com.coco.payment.persistence.enumerator.PaymentTransactionStatus

@Alias("payment_transaction")
data class PaymentTransaction(
    var id: Long?,
    val paymentKey: String,
    val orderSeq: Long,
    val moid: String,
    val tid: String?,
    val amount: Long,
    val status: PaymentTransactionStatus,
    val approvedAt: Instant?,
    val failCode: PaymentFailCode?,
    val failMessage: String?,
    var createdAt: Instant?,
    var updatedAt: Instant?,
) {
    val isSuccess: Boolean get() = status == PaymentTransactionStatus.SUCCESS

    val isFailed: Boolean get() = status == PaymentTransactionStatus.FAILED

    val isPending: Boolean get() = status == PaymentTransactionStatus.PENDING

    fun hasSameAmount(amount: Long) = this.amount == amount

    // 확정 기한을 넘겼는가. 넘기면 승인이 성공했더라도 되돌린다.
    fun isExpired(now: Instant) = createdAt!!.plusSeconds(CONFIRM_DEADLINE_SECONDS) <= now

    companion object {
        // 승인 호출이 끝났다고 볼 수 있는 시간. 연결 10초 + 읽기 60초(Toss 권장 하한)보다 크게 잡는다.
        // 이 시간 안의 거래를 조회하면 진행 중인 승인과 겹쳐 CAS 0행이 되고 클라이언트는 500을 받는다.
        // 다만 상한이 아니라 확률적 가정이다. 읽기 60초는 호출 총시간이 아니라 읽기 사이 간격이라
        // (SimpleClientHttpRequestFactory의 SO_TIMEOUT), Toss가 바이트를 조금씩 흘리면 70초를 넘는다.
        private const val APPROVE_DONE_AFTER_SECONDS = 90L

        // 배송 마감 전에 결론이 나야 하므로 이 시간 안에 성공이나 실패로 끝낸다.
        private const val CONFIRM_DEADLINE_SECONDS = 300L

        // 이 시각 이전에 만들어진 미확정 거래는 승인이 끝났다고 보고 조회 대상으로 삼는다.
        fun approveDoneBefore(now: Instant): Instant = now.minusSeconds(APPROVE_DONE_AFTER_SECONDS)
    }
}
