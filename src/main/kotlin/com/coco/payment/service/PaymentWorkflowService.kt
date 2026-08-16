package com.coco.payment.service

import com.coco.payment.service.dto.BillingOrderItem
import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.service.dto.BillingPaymentItem
import com.coco.payment.handler.paymentgateway.toss.dto.TossBillingPaymentResult
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.repository.CompanyBillingKeyRepository
import com.coco.payment.service.dto.PrepareBillingPaymentResult
import com.coco.payment.service.exception.DeliveryDateChangedException
import com.coco.payment.service.exception.OrderAlreadyPaidException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PaymentWorkflowService(
    private val orderService: OrderService,
    private val paymentTransactionService: PaymentTransactionService,
    private val companyBillingKeyRepository: CompanyBillingKeyRepository,
    @Value("\${payment.pending-timeout-seconds}")
    private val pendingTimeoutSeconds: Long,
) {
    fun findByPaymentKey(paymentKey: String) = paymentTransactionService.findByPaymentKey(paymentKey)

    @Transactional
    fun prepare(command: BillingPaymentCommand): PrepareBillingPaymentResult {
        val orderItems = orderService.resolveOrderItems(command.items)
        // 클라이언트의 totalPrice는 "사용자가 확인한 금액"이므로, 재계산 값과 다르면 승인 전에 거부한다.
        val computedTotal = orderItems.sumOf { it.unitPrice * it.quantity }
        require(computedTotal == command.totalPrice) {
            "Total price mismatch: requested ${command.totalPrice} but computed $computedTotal"
        }

        // 화면에서 본 보장일이 지금도 약속 가능한지 확인한다. 마감(22시)을 넘겼으면
        // 지킬 수 없는 보장이므로 승인 전에 거부하고, 클라이언트가 새 값을 받아가게 한다.
        val deliveryDate = orderService.computeDeliveryDate()
        if (command.deliveryDate != deliveryDate) {
            throw DeliveryDateChangedException("배송 보장일이 변경되었습니다. 다시 확인해 주세요.")
        }

        val existingOrder = orderService.findByOrderKeyForUpdate(command.orderKey)
        val order = if (existingOrder != null) {
            require(existingOrder.companySeq == command.companySeq && existingOrder.totalPrice == command.totalPrice) {
                "Order key is already associated with a different order"
            }
            // 합계가 같아도 상품 구성은 다를 수 있다(예: 6500x2 와 5000+3800+4200).
            // 기존 주문의 항목은 갱신하지 않으므로, 다르면 저장된 내용과 어긋난 채로 승인된다.
            require(canonicalize(orderService.findItems(existingOrder.id!!)) == canonicalize(orderItems)) {
                "Order key is already associated with different order items"
            }
            existingOrder
        } else {
            val orderId = orderService.createPendingOrder(command.orderKey, command.companySeq, command.totalPrice, deliveryDate, orderItems)
            orderService.findById(orderId) ?: error("Created order not found: $orderId")
        }

        // PENDING 거래가 없어도 주문 자체가 이미 결제 완료일 수 있다(예: 폴링이 끝난 뒤 재처리가 확정한 경우).
        // 이때 같은 orderKey로 다시 들어오면 중복 승인이 되므로 주문 상태로 막는다.
        if (order.isPaid) throw OrderAlreadyPaidException("이미 결제가 완료된 주문입니다.")

        // 결제 전까지 보장일은 계약이 아니다. 마감을 넘겨 낡은 값이 저장돼 있으면
        // (예: 21:58 생성 주문을 22:10에 재시도) 지금 약속 가능한 값으로 갱신한다.
        if (existingOrder != null && existingOrder.deliveryDate != deliveryDate) {
            orderService.updateDeliveryDate(order.id!!, deliveryDate)
        }

        val pending = paymentTransactionService.findPendingByOrderSeq(order.id!!)
        if (pending != null) {
            return PrepareBillingPaymentResult.AlreadyPending(order.id!!, command.orderKey, pending.paymentKey, pending.status, pending.tid)
        }

        val moid = command.paymentKey
        val expiredAt = Instant.now().plusSeconds(pendingTimeoutSeconds)
        val paymentTransactionId = paymentTransactionService.createPending(command.paymentKey, order.id!!, moid, command.totalPrice, expiredAt)
        val billingKey = companyBillingKeyRepository.findByCompanySeqAndPaymentSystem(command.companySeq, PaymentSystem.TOSS)
            ?: throw IllegalArgumentException("Toss billing key not found for company: ${command.companySeq}")
        return PrepareBillingPaymentResult.Ready(order.id!!, paymentTransactionId, command.orderKey, command.paymentKey, billingKey.billingKey, billingKey.customerKey, moid, command.orderName, command.totalPrice)
    }

    // 순서와 무관하게 비교하기 위한 정렬된 표현.
    private fun canonicalize(items: List<BillingOrderItem>) =
        items.map { "${it.itemName}:${it.unitPrice}:${it.quantity}" }.sorted()

    // TODO: 결제가 늦게 확정되면(재처리가 PENDING을 뒤늦게 성공으로 확정) 주문의 배송 보장일이
    //  이미 지킬 수 없는 값일 수 있다. 확정 시점(completeByTransactionId 포함)에 보장일을 재계산해서
    //  어긋나면 취소한다 — 도착 보장 정책. 취소 API가 생기는 망취소 단계에서 구현.
    //  취소하지 않는 대안도 검토할 것: 쿠팡식으로 다음 회차에 보내고 보상하는 모델. 단 그 경우
    //  약속한 날짜와 실제 태울 날짜를 둘 다 저장해야 한다(약속이 남아 있어야 보상 근거가 된다).
    @Transactional
    fun complete(prepared: PrepareBillingPaymentResult.Ready, result: TossBillingPaymentResult) {
        paymentTransactionService.complete(prepared.paymentTransactionId, result.tid)
        orderService.markPaid(prepared.orderId)
    }

    // 실패는 "이번 시도"의 결과일 뿐 주문의 종료 상태가 아니다. 같은 orderKey로 재시도할 수 있으므로
    // 주문은 결제될 때까지 PENDING_PAYMENT로 두고, 시도별 결과는 payment_transaction에만 남긴다.
    @Transactional
    fun fail(prepared: PrepareBillingPaymentResult.Ready, failCode: String?, failMessage: String?) {
        paymentTransactionService.fail(prepared.paymentTransactionId, failCode, failMessage)
    }

    @Transactional
    fun completeByTransactionId(paymentTransactionId: Long, tid: String) {
        val transaction = paymentTransactionService.findById(paymentTransactionId)
            ?: error("Payment transaction not found: $paymentTransactionId")
        paymentTransactionService.complete(paymentTransactionId, tid)
        orderService.markPaid(transaction.orderSeq)
    }

    @Transactional
    fun failByTransactionId(paymentTransactionId: Long, failCode: String?, failMessage: String?) {
        paymentTransactionService.fail(paymentTransactionId, failCode, failMessage)
    }
}
