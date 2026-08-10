package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.toss.TossBillingKeyHandler
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.CompanyBillingKey
import com.coco.payment.persistence.repository.CompanyBillingKeyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyBillingKeyService(
    private val companyBillingKeyRepository: CompanyBillingKeyRepository,
    private val tossBillingKeyHandler: TossBillingKeyHandler,
) {
    @Transactional
    fun registerTossBillingKey(companySeq: Long, customerKey: String, authKey: String): CompanyBillingKey {
        val billingKey = tossBillingKeyHandler.issue(customerKey, authKey)
        val existing = companyBillingKeyRepository.findByCompanySeqAndPaymentSystem(companySeq, PaymentSystem.TOSS)
        if (existing != null) {
            check(companyBillingKeyRepository.updateBillingKey(existing.id!!, billingKey) == 1) { "Failed to update billing key" }
            return existing.copy(billingKey = billingKey)
        }
        val companyBillingKey = CompanyBillingKey(null, companySeq, PaymentSystem.TOSS, customerKey, billingKey, null, null)
        check(companyBillingKeyRepository.insert(companyBillingKey) == 1) { "Failed to insert billing key" }
        return companyBillingKey
    }
}
