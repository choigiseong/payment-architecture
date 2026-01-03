package com.coco.payment.persistence.enumerator

enum class InvoiceType(val description: String) {
    PREPAYMENT("Prepayment Invoice"),
    SUBSCRIPTION("Subscription Invoice");
}