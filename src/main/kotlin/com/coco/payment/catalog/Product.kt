package com.coco.payment.catalog

data class Product(val id: Long, val name: String, val price: Long)

object ProductCatalog {
    private val products = listOf(
        Product(1, "아메리카노", 4500),
        Product(2, "카페라떼", 5000),
        Product(3, "크루아상", 3800),
        Product(4, "베이글", 4200),
        Product(5, "샌드위치", 6500),
    )

    fun all(): List<Product> = products
}
