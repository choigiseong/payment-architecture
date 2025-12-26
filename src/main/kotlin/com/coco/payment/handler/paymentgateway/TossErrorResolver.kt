package com.coco.payment.handler.paymentgateway

import com.coco.payment.handler.paymentgateway.dto.PgResult
import org.springframework.http.HttpStatusCode

class TossErrorResolver {


    fun resolve(
        status: HttpStatusCode,
        rawCode: String?
    ): PgResult<Nothing> {

        return when (status.value()) {

            401, 403 -> {
                if (rawCode == "FORBIDDEN_CONSECUTIVE_REQUEST") {
                    PgResult.Retryable(
                        PgError(PgErrorCode.RATE_LIMIT, "반복 요청 제한", rawCode)
                    )
                } else {
                    PgResult.Critical(
                        PgError(PgErrorCode.AUTH, "인증 오류", rawCode)
                    )
                }
            }

            400, 404 -> PgResult.Fail(
                PgError(PgErrorCode.BUSINESS, "결제 조건 불일치", rawCode)
            )

            else -> PgResult.Retryable(
                PgError(PgErrorCode.UNKNOWN, "PG 내부 오류", rawCode)
            )
        }
    }

}
