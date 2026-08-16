package com.coco.payment.service

import com.coco.payment.handler.paymentgateway.toss.TossPaymentHandler
import com.coco.payment.persistence.enumerator.PaymentSystem
import com.coco.payment.persistence.model.CompanyBillingKey
import com.coco.payment.persistence.repository.CompanyBillingKeyRepository
import org.springframework.stereotype.Service

@Service
class CompanyBillingKeyService(
    private val companyBillingKeyRepository: CompanyBillingKeyRepository,
    private val tossPaymentHandler: TossPaymentHandler,
) {
    // 트랜잭션을 걸지 않는다. 분기마다 쓰기가 하나뿐이라 묶을 것이 없고,
    // 트랜잭션을 열면 Toss 발급 요청이 끝날 때까지 DB 커넥션을 붙잡고 있게 된다.
    fun registerTossBillingKey(companySeq: Long, customerKey: String, authKey: String): CompanyBillingKey {
        val billingKey = tossPaymentHandler.issue(customerKey, authKey)
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
