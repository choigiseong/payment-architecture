package com.coco.payment.controller

import com.coco.payment.controller.dto.CheckoutPreviewLine
import com.coco.payment.controller.dto.CheckoutPreviewRequest
import com.coco.payment.controller.dto.CheckoutPreviewResponse
import com.coco.payment.service.OrderService
import com.coco.payment.service.dto.BillingPaymentItem
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/checkout/preview")
class CheckoutPreviewController(private val orderService: OrderService) {
    @PostMapping
    fun preview(@Valid @RequestBody request: CheckoutPreviewRequest): CheckoutPreviewResponse {
        val resolved = orderService.resolveOrderItems(request.items.map { BillingPaymentItem(it.productId, it.quantity) })
        val lines = request.items.zip(resolved) { requested, item ->
            CheckoutPreviewLine(requested.productId, item.itemName, item.unitPrice, item.quantity, item.unitPrice * item.quantity)
        }
        return CheckoutPreviewResponse(
            orderService.computeDeliveryDate(),
            lines.sumOf { it.lineTotal },
            lines,
        )
    }
}
