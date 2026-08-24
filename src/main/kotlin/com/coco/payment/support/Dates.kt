package com.coco.payment.support

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 배송 마감처럼 "몇 시인가"가 기준인 계산은 모두 이 시간대를 쓴다.
// 시점만 다루는 값(payment_transaction의 만료 시각 등)은 Instant라 시간대와 무관하다.
object Dates {
    // @Scheduled(zone = ...)처럼 컴파일 상수가 필요한 곳에서 쓴다.
    const val ZONE_ID = "Asia/Seoul"

    private val SEOUL: ZoneId = ZoneId.of(ZONE_ID)

    fun now(): LocalDateTime = LocalDateTime.now(SEOUL)

    fun today(): LocalDate = LocalDate.now(SEOUL)

    // 서울 벽시계 값을 시점으로 바꾼다. now()·today()는 서울 값을 만들어 주지만 이쪽은 입력이
    // 서울이라고 가정하는 쪽이라 이름에 시간대를 드러낸다. 규칙이 호출부로 흩어지지 않게 SEOUL은 감춘다.
    fun seoulToInstant(dateTime: LocalDateTime): Instant = dateTime.atZone(SEOUL).toInstant()

    // LocalDateTime.toString()은 초가 0이면 초를 생략해 포맷이 흔들린다. 외부로 보내는 로컬 시각은 이 형식으로 고정한다.
    fun format(dateTime: LocalDateTime): String = dateTime.format(LOCAL_DATE_TIME_FORMAT)

    private val LOCAL_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
}
