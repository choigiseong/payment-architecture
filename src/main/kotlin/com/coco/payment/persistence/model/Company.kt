package com.coco.payment.persistence.model

import java.time.Instant
import org.apache.ibatis.type.Alias

@Alias("company")
data class Company(
    var id: Long?,
    val companyName: String,
    var createdAt: Instant?,
    var updatedAt: Instant?,
)
