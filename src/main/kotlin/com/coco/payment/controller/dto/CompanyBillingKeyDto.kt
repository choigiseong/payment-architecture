package com.coco.payment.controller.dto

import com.coco.payment.persistence.model.CompanyBillingKey
import jakarta.validation.constraints.NotBlank

data class IssueBillingKeyRequest(
    @field:NotBlank val customerKey: String,
    @field:NotBlank val authKey: String,
)

data class CompanyBillingKeyResponse(val companySeq: Long, val customerKey: String) {
    companion object {
        fun of(companyBillingKey: CompanyBillingKey) =
            CompanyBillingKeyResponse(companyBillingKey.companySeq, companyBillingKey.customerKey)
    }
}
