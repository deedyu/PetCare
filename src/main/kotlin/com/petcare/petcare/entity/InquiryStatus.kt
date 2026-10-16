package com.petcare.petcare.entity

enum class InquiryStatus(val label: String) {
    OPEN("접수"),
    IN_PROGRESS("처리 중"),
    ANSWERED("답변 완료")
}
