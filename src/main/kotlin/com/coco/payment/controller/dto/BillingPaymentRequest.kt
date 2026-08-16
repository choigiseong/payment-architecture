package com.coco.payment.controller.dto

import com.coco.payment.service.dto.BillingPaymentCommand
import com.coco.payment.service.dto.BillingPaymentItem
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
    fun toCommand() = BillingPaymentCommand.of(companySeq, orderKey, paymentKey, orderName, totalPrice, items.map { BillingPaymentItem(it.productId, it.quantity) })
}

data class BillingPaymentItemRequest(
    @field:Positive val productId: Long,
    @field:Positive val quantity: Int,
)
