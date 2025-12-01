package com.coco.payment.controller

import com.coco.payment.service.CustomerService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@Controller
class PaymentController(
    private val customerService: CustomerService,
    @Value("\${payment.toss.api-key}")
    private val tossPayApiKey: String
) {
    @RequestMapping(value = ["/"], method = [RequestMethod.GET])
    fun index(): String {
        return "/widget/checkout"
    }

    @RequestMapping(value = ["/fail"], method = [RequestMethod.GET])
    fun failPayment(request: HttpServletRequest, model: Model): String {
        model.addAttribute("code", request.getParameter("code"))
        model.addAttribute("message", request.getParameter("message"))
        return "/fail"
    }
}