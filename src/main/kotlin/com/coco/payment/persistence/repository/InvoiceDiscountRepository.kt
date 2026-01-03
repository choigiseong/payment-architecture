package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.InvoiceDiscount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface InvoiceDiscountRepository : JpaRepository<InvoiceDiscount, Long> {

    @Query(
        value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM invoice_discount
            WHERE invoice_seq = :invoiceSeq
        """,
        nativeQuery = true
    )
    fun sumAmountByInvoice(invoiceSeq: Long): Long

    fun findByInvoiceSeq(invoiceSeq: Long): List<InvoiceDiscount>
}
