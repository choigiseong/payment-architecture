package com.coco.payment.persistence.model

class Ledger(
    val id: Long,
    val customerSeq: String,
    val beforeBalance: Long,
    val balance: Long,
    val amount: Long
) {
}