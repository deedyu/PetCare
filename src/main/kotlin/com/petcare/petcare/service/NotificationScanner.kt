package com.petcare.petcare.service

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 화면을 열지 않아도 지난 알림 시각을 모은다.
 * 테스트에서는 끄고, 서비스 메서드를 직접 호출한다.
 */
@Component
@ConditionalOnProperty(name = ["petcare.notifications.scan"], havingValue = "true", matchIfMissing = true)
class NotificationScanner(
    private val notificationService: NotificationService
) {
    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    fun scan() {
        notificationService.deliverDueReminders(LocalDateTime.now())
    }
}
