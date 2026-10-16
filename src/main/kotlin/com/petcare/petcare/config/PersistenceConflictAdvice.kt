package com.petcare.petcare.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * 같은 행을 두 사람이 저장하면 나중 저장만 거절한다. 먼저 저장된 값은 남긴다.
 */
@ControllerAdvice
class PersistenceConflictAdvice {
    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun conflict(request: HttpServletRequest): ResponseEntity<String> {
        val message = "다른 사용자가 먼저 저장했습니다. 화면을 다시 불러온 뒤 수정해 주세요."
        val path = request.requestURI.orEmpty()
        if (path.startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(message)
        }
        val html = """
            <!DOCTYPE html>
            <html lang="ko">
            <head><meta charset="UTF-8"><title>저장 충돌</title></head>
            <body>
            <p>$message</p>
            <p><a href="/">홈으로</a></p>
            </body>
            </html>
        """.trimIndent()
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .contentType(MediaType.TEXT_HTML)
            .body(html)
    }
}
