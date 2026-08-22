package com.coco.payment.service

import com.coco.payment.persistence.enumerator.OrderStatus
import com.coco.payment.support.Dates
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class OrderHandoffService(private val orderService: OrderService) {
    // 마감(22시) 직후 다음날 배송분을 외부 업체로 넘긴다. 붙일 외부 시스템이 아직 없어
    // 상태 전이가 전달을 대신한다. PAID만 고르므로 이미 넘긴 주문은 다시 걸리지 않고,
    // 결제가 확정되지 않은 주문은 애초에 대상이 아니다(5분 룰이 취소로 보낸다).
    @Scheduled(cron = "\${payment.delivery.handoff-cron}", zone = Dates.ZONE_ID)
    fun handoffTomorrowDeliveries() {
        val deliveryDate = Dates.today().plusDays(1)
        for (order in orderService.findByDeliveryDateAndStatus(deliveryDate, OrderStatus.PAID)) {
            // 건별로 격리한다. 하나가 실패해도 나머지가 이번 회차에서 빠지면 안 된다.
            try {
                orderService.markPreparing(order.id!!)
            } catch (exception: Exception) {
                log.error("Failed to hand off order: ${order.id}", exception)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderHandoffService::class.java)
    }
}
