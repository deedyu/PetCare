package com.petcare.petcare

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

/** 잘못된 날짜·시간은 서버 오류 대신 입력 오류로 돌린다. */
fun localDateOrThrow(value: String?): LocalDate? {
    val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return try {
        LocalDate.parse(text)
    } catch (_: DateTimeParseException) {
        throw IllegalArgumentException("날짜 형식이 올바르지 않습니다.")
    }
}

fun localTimeOrThrow(value: String?): LocalTime? {
    val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return try {
        LocalTime.parse(text)
    } catch (_: DateTimeParseException) {
        throw IllegalArgumentException("시간 형식이 올바르지 않습니다.")
    }
}
