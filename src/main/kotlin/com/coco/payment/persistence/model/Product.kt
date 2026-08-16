package com.coco.payment.persistence.model

import java.time.Instant
import org.apache.ibatis.type.Alias

@Alias("product")
data class Product(
    var id: Long?,
    val name: String,
    val price: Long,
    var createdAt: Instant?,
    var updatedAt: Instant?,
)
