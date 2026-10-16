package com.petcare.petcare.service

import com.petcare.petcare.entity.AccountStatus
import com.petcare.petcare.entity.InquiryStatus
import com.petcare.petcare.entity.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
class AdminOpsTest {
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var announcementService: AnnouncementService
    @Autowired lateinit var inquiryService: InquiryService
    @Autowired lateinit var userDetailsService: UserDetailsService
    @Autowired lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun suspendedAccountCannotSignInAndInquiryStaysWithItsAuthor() {
        val admin = userService.createUser("ops-admin", "abcd1234", "운영", role = Role.ADMIN)
        val member = userService.createUser("ops-member", "abcd1234", "회원", role = Role.USER)
        val other = userService.createUser("ops-other", "abcd1234", "다른회원", role = Role.USER)

        announcementService.create(admin.id!!, "나중에", "일반 공지", pinned = false)
        announcementService.create(admin.id!!, "먼저", "고정 공지", pinned = true)
        val notices = announcementService.list()
        assertEquals("먼저", notices.first().title)
        assertTrue(notices.first().pinned)

        val inquiry = inquiryService.create(member.id!!, "밥 시간", "알림이 안 와요")
        assertFailsWith<IllegalArgumentException> {
            inquiryService.requireOwn(other.id!!, inquiry.id!!)
        }
        inquiryService.reply(admin.id!!, inquiry.id!!, "확인해 보겠습니다.")
        assertEquals(InquiryStatus.ANSWERED, inquiryService.requireAny(inquiry.id!!).status)
        assertEquals(1, inquiryService.replies(inquiry.id!!).size)

        userService.changeStatus(member.id!!, AccountStatus.SUSPENDED)
        assertFalse(userDetailsService.loadUserByUsername("ops-member").isEnabled)
        userService.deleteUser(member.id!!)
        entityManager.flush()
        entityManager.clear()
        assertFailsWith<IllegalArgumentException> {
            inquiryService.requireAny(inquiry.id!!)
        }
        assertFailsWith<IllegalArgumentException> {
            userService.changeStatus(admin.id!!, AccountStatus.SUSPENDED)
        }
    }
}
