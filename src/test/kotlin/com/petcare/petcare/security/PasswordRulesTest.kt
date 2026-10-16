package com.petcare.petcare.security

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordRulesTest {
    @Test
    fun acceptsPasswordWithLetterAndDigit() {
        PasswordRules.check("abcd1234")
    }

    @Test
    fun rejectsShortPassword() {
        assertThrows(IllegalArgumentException::class.java) {
            PasswordRules.check("ab12")
        }
    }

    @Test
    fun detectsBcryptHash() {
        assertTrue(PasswordRules.isBcrypt("\$2a\$10\$abcdefghijklmnopqrstuv"))
        assertFalse(PasswordRules.isBcrypt("admin"))
    }
}
