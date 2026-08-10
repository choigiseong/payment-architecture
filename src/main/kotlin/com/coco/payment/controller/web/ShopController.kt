package com.coco.payment.controller.web

import com.coco.payment.catalog.ProductCatalog
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ShopController {
    @GetMapping("/shop")
    fun shop(model: Model): String {
        model.addAttribute("products", ProductCatalog.all())
        return "shop"
    }

    @GetMapping("/cart")
    fun cart(): String = "cart"

    @GetMapping("/checkout")
    fun checkout(): String = "checkout"

    @GetMapping("/checkout/result")
    fun checkoutResult(): String = "checkout-result"
}
