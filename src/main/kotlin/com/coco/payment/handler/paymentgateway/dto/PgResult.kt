package com.coco.payment.handler.paymentgateway.dto

import com.coco.payment.handler.paymentgateway.PgError

sealed interface PgResult<out T> {

    /**
     * 내일 여기서 이어서
     * https://tech.kakaopay.com/post/msa-transaction/#:~:text=%EA%B7%B8%EB%9E%98%EC%84%9C%20%EC%83%81%ED%92%88%EA%B6%8C%EC%97%90%EC%84%9C%EB%8A%94%20%EC%B2%98%EC%9D%8C%20%EC%9A%94%EC%B2%AD%EC%9D%98%20%EC%98%88%EC%99%B8%EC%83%81%ED%99%A9%EA%B9%8C%EC%A7%80%EB%8A%94%20%EA%B3%A0%EA%B0%9D%20%EC%9D%91%EB%8B%B5%EC%9D%B4,%ED%95%B4%EB%8B%B9%20%EA%B2%B0%EC%A0%9C%EA%B1%B4%EC%9D%B4%20%EC%95%8C%EC%88%98%EC%97%86%EC%9D%8C%EC%9C%BC%EB%A1%9C%20%EC%A0%80%EC%9E%A5%EB%90%9C%20%ED%8A%B8%EB%9E%9C%EC%9E%AD%EC%85%98%EC%9D%B8%EC%A7%80%20%ED%99%95%EC%9D%B8%ED%95%98%EA%B3%A0%20%ED%9B%84%EC%B2%98%EB%A6%AC%EB%A5%BC
     */

    data class Success<T>(
        val value: T
    ) : PgResult<T>

    data class Retryable(
        val error: PgError
    ) : PgResult<Nothing>

    data class Fail(
        val error: PgError
    ) : PgResult<Nothing>

    data class Critical(
        val error: PgError
    ) : PgResult<Nothing>
}
