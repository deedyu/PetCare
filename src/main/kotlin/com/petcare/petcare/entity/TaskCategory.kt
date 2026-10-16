package com.petcare.petcare.entity

enum class TaskCategory(val label: String) {
    MEAL("식사"),
    WALK("산책"),
    HEALTH("건강"),
    MEDICINE("약 복용"),
    GROOMING("목욕 및 미용"),
    HOSPITAL("병원"),
    OTHER("기타");

    companion object {
        fun from(raw: String?): TaskCategory? = entries.find { it.name == raw }
    }
}
