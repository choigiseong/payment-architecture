package com.coco.payment.controller.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate

data class CheckoutPreviewRequest(
    @field:NotEmpty
    @field:Valid
    val items: List<BillingPaymentItemRequest>,
)

// 화면에 그릴 값과 결제 요청에 실을 값을 모두 서버가 계산해 내려준다.
// 클라이언트가 같은 규칙을 복제하지 않아도 되고, 시계 차이로 어긋날 일도 없다.
data class CheckoutPreviewResponse(
    val deliveryDate: LocalDate,
    val totalPrice: Long,
    val items: List<CheckoutPreviewLine>,
)

data class CheckoutPreviewLine(
    val productId: Long,
    val name: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineTotal: Long,
)
