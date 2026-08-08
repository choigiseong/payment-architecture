package com.coco.payment.persistence.enumerator

enum class PaymentTransactionStatus(val code: Int) {
    PENDING(1),
    SUCCESS(2),
    FAILED(3),
}
