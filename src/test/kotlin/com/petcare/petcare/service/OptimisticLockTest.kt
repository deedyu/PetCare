package com.petcare.petcare.service

import com.petcare.petcare.repository.PetRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.support.TransactionTemplate

@SpringBootTest
class OptimisticLockTest {
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var petService: PetService
    @Autowired lateinit var petRepository: PetRepository
    @Autowired lateinit var entityManager: EntityManager
    @Autowired lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun secondSaveKeepsTheFirstValue() {
        val user = userService.createUser("lock-user", "abcd1234", "잠금")
        val pet = petService.createPet(user, "화면콩", "고양이", 2)
        val petId = pet.id!!

        val stale = transactionTemplate.execute {
            val loaded = petRepository.findById(petId).orElseThrow()
            entityManager.detach(loaded)
            loaded
        }!!

        transactionTemplate.executeWithoutResult {
            val current = petRepository.findById(petId).orElseThrow()
            current.name = "먼저저장"
            petRepository.save(current)
        }

        var rejected = false
        try {
            transactionTemplate.executeWithoutResult {
                stale.name = "나중저장"
                petRepository.saveAndFlush(stale)
            }
        } catch (e: RuntimeException) {
            rejected = hasLockFailure(e)
            if (!rejected) {
                throw e
            }
        }
        assertEquals(true, rejected)
        assertEquals("먼저저장", petRepository.findById(petId).orElseThrow().name)
    }

    private fun hasLockFailure(error: Throwable?): Boolean {
        var current = error
        while (current != null) {
            if (current is OptimisticLockingFailureException) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
