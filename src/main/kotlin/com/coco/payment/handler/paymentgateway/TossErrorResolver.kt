package com.coco.payment.handler.paymentgateway

import com.coco.payment.handler.paymentgateway.dto.PgError
import com.coco.payment.handler.paymentgateway.dto.PgResult

class TossErrorResolver {


    fun findTransactionErrorResolver(
        status: Int,
        rawCode: String
    ): PgResult<Nothing> {

        return when (status) {
            401, 403 -> {
                if (rawCode == "FORBIDDEN_CONSECUTIVE_REQUEST") {
                    PgResult.Retryable(
                        PgError(status.toString(), rawCode)
                    )
                } else {
                    PgResult.Critical(
                        PgError(status.toString(), rawCode)
                    )
                }
            }

            400, 404 -> PgResult.Fail(
                PgError(status.toString(), rawCode)
            )

            else -> PgResult.Retryable(
                PgError(status.toString(), rawCode)
            )
        }
    }

}
