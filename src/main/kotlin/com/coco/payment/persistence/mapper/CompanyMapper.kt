package com.coco.payment.persistence.mapper

import com.coco.payment.persistence.model.Company
import org.apache.ibatis.annotations.Param

interface CompanyMapper {
    fun insert(company: Company): Int

    fun findById(@Param("id") id: Long): Company?
}
