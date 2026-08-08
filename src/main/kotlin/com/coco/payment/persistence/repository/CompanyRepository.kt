package com.coco.payment.persistence.repository

import com.coco.payment.persistence.model.Company
import org.apache.ibatis.annotations.Param

interface CompanyRepository {
    fun insert(company: Company): Int

    fun findById(@Param("id") id: Long): Company?
}
