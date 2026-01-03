package com.coco.payment.service

import com.coco.payment.persistence.enumerator.DiscountType
import com.coco.payment.persistence.repository.InvoiceDiscountRepository
import org.springframework.stereotype.Service

@Service
class InvoiceDiscountService(
    private val invoiceDiscountRepository: InvoiceDiscountRepository,
) {

    fun create(
        invoiceSeq: Long,
        type: DiscountType,
        refSeq: Long,
        amount: Long,
    ) {

    }
}