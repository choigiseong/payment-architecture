package com.coco.payment.service.dto

data class BillingPaymentCommand(
    val companySeq: Long,
    val orderKey: String,
    val paymentKey: String,
    val orderName: String,
    val totalPrice: Long,
    val items: List<BillingPaymentItem>,
)

{
    companion object {
        // TODO: 인증이 붙으면 키에 companySeq를 섞어 회사별 네임스페이스를 만든다
        //  (order_key, payment_key 모두 "${companySeq}-${클라이언트 키}"). 파생 키가 전역 유일하면서
        //  회사별로 갈라져 스키마와 조회 시그니처는 그대로 둘 수 있고, moid는 payment_key라 손댈 것이 없다.
        //  인증 전에 넣으면 안 된다 — 승인 응답을 못 받은 클라이언트에는 원본 키뿐이라 결과 페이지가 404가 되고,
        //  이를 피하려면 클라이언트가 파생 규칙을 복제해야 하는데 인증이 붙으면 지워야 할 코드다.
        fun of(
            companySeq: Long,
            orderKey: String,
            paymentKey: String,
            orderName: String,
            totalPrice: Long,
            items: List<BillingPaymentItem>,
        ) = BillingPaymentCommand(companySeq, orderKey, paymentKey, orderName, totalPrice, items)
    }
}

data class BillingPaymentItem(val productId: Long, val quantity: Int)

data class BillingOrderItem(val itemName: String, val unitPrice: Long, val quantity: Int)
