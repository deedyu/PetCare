package com.petcare.petcare.entity

/**
 * 서비스 권한. 가족 그룹의 OWNER/MEMBER와는 별개다.
 * ADMIN은 운영 화면만 쓰고, 다른 회원의 건강 기록은 보지 않는다.
 */
enum class Role {
    USER,
    ADMIN
}
