package com.coco.payment.controller.dto

import com.coco.payment.catalog.ProductCatalog
import com.coco.payment.service.dto.BillingOrderItem
import com.coco.payment.service.dto.BillingPaymentCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class BillingPaymentRequest(
    val companySeq: Long,
    @field:NotBlank
    val orderKey: String,
    @field:NotBlank
    val paymentKey: String,
    val orderName: String,
    @field:Positive
    val totalPrice: Long,
    @field:NotEmpty
    @field:Valid
    val items: List<BillingPaymentItemRequest>,
) {
    // 이름과 가격은 클라이언트 값을 받지 않고 서버 카탈로그에서 조회한다.
    // 클라이언트의 totalPrice는 "사용자가 확인한 금액"이므로, 서버 재계산 값과 다르면 승인 전에 거부한다.
    fun toCommand(): BillingPaymentCommand {
        val orderItems = items.map { item ->
            val product = ProductCatalog.findById(item.productId)
                ?: throw IllegalArgumentException("Unknown product: ${item.productId}")
            BillingOrderItem(product.name, product.price, item.quantity)
        }
        val computedTotal = orderItems.sumOf { it.unitPrice * it.quantity }
        require(computedTotal == totalPrice) {
            "Total price mismatch: requested $totalPrice but computed $computedTotal"
        }
        return BillingPaymentCommand.of(companySeq, orderKey, paymentKey, orderName, computedTotal, orderItems)
    }
}

data class BillingPaymentItemRequest(
    @field:Positive val productId: Long,
    @field:Positive val quantity: Int,
)
