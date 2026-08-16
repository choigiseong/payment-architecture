package com.coco.payment.controller

import com.coco.payment.controller.dto.ApiErrorCode
import com.coco.payment.service.exception.DeliveryDateChangedException
import com.coco.payment.service.exception.OrderAlreadyPaidException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

// IllegalStateException 등 여기서 다루지 않는 예외는 서블릿의 /error로 흘러가 500이 된다.
// 서버 불변식이 깨진 경우라 클라이언트에게 알릴 내용이 없으므로 그대로 둔다.
@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(OrderAlreadyPaidException::class)
    fun handleOrderAlreadyPaid(e: OrderAlreadyPaidException): ProblemDetail =
        problemDetail(HttpStatus.CONFLICT, ApiErrorCode.ORDER_ALREADY_PAID, e.message)

    // 클라이언트가 preview를 다시 받아 화면을 갱신하면 해소되므로 사유를 그대로 알려준다.
    @ExceptionHandler(DeliveryDateChangedException::class)
    fun handleDeliveryDateChanged(e: DeliveryDateChangedException): ProblemDetail =
        problemDetail(HttpStatus.CONFLICT, ApiErrorCode.DELIVERY_DATE_CHANGED, e.message)

    // 예외 메시지는 개발자용이라 그대로 노출하지 않는다. 사유별 안내가 필요해지면 그때 예외를 나눈다.
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ProblemDetail {
        log.warn("Bad payment request", e)
        return problemDetail(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_REQUEST, "요청을 처리할 수 없습니다. 장바구니를 확인해 주세요.")
    }

    private fun problemDetail(status: HttpStatus, code: ApiErrorCode, detail: String?) =
        ProblemDetail.forStatusAndDetail(status, detail).apply { type = URI.create(code.value.toString()) }

    companion object {
        private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
