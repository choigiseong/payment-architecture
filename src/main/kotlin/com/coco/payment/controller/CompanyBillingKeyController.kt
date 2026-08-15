package com.coco.payment.controller

import com.coco.payment.controller.dto.CompanyBillingKeyResponse
import com.coco.payment.controller.dto.IssueBillingKeyRequest
import com.coco.payment.service.CompanyBillingKeyService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/companies/{companySeq}/billing-keys")
class CompanyBillingKeyController(private val companyBillingKeyService: CompanyBillingKeyService) {
    @PostMapping
    fun issue(@PathVariable companySeq: Long, @Valid @RequestBody request: IssueBillingKeyRequest): CompanyBillingKeyResponse =
        CompanyBillingKeyResponse.of(
            companyBillingKeyService.registerTossBillingKey(companySeq, request.customerKey, request.authKey)
        )
}
