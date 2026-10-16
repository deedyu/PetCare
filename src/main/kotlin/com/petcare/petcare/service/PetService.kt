package com.petcare.petcare.service

import com.petcare.petcare.entity.Pet
import com.petcare.petcare.entity.User
import com.petcare.petcare.repository.HealthRecordRepository
import com.petcare.petcare.repository.PetRepository
import com.petcare.petcare.repository.TaskOccurrenceRepository
import com.petcare.petcare.repository.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PetService(
    private val petRepository: PetRepository,
    private val taskRepository: TaskRepository,
    private val occurrenceRepository: TaskOccurrenceRepository,
    private val healthRecordRepository: HealthRecordRepository
) {

    // ✅ 사용자별 펫 목록 조회
    fun getPetsByUser(userId: Long): List<Pet> {
        return petRepository.findByUserIdOrderByIdDesc(userId)
    }

    // ✅ 사용자별 최신 펫 목록 조회 (최대 3개)
    fun getLatestPetsByUser(userId: Long, limit: Int = 3): List<Pet> {
        return petRepository.findTop3ByUserIdOrderByIdDesc(userId)
    }

    fun getVisiblePets(userId: Long): List<Pet> {
        return petRepository.findVisibleToUser(userId)
    }

    // ✅ 전체 펫 목록 조회 (관리자용)
    fun getAllPets(): List<Pet> {
        return petRepository.findAll()
    }

    // ✅ 단일 펫 조회 (Task 쪽에서 사용)
    fun getPet(petId: Long): Pet {
        return petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("pet not found: $petId") }
    }

    // ✅ 펫 생성 (사용자 정보 포함)
    fun createPet(user: User, name: String, type: String, age: Int, imageUrl: String? = null): Pet {
        val checked = requirePet(name, type, age)
        val pet = Pet(
            name = checked.first,
            type = checked.second,
            age = age,
            imageUrl = imageUrl,
            user = user
        )
        return petRepository.save(pet)
    }

    // ✅ 펫 수정 (사용자 확인 포함)
    fun updatePet(userId: Long, petId: Long, name: String, type: String, age: Int): Pet {
        val pet = petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("pet not found: $petId") }
        
        // 사용자 확인
        if (pet.user?.id != userId) {
            throw IllegalArgumentException("unauthorized: pet does not belong to user")
        }

        val checked = requirePet(name, type, age)
        pet.name = checked.first
        pet.type = checked.second
        pet.age = age
        return petRepository.save(pet)
    }

    private fun requirePet(name: String, type: String, age: Int): Pair<String, String> {
        val trimmedName = name.trim()
        val trimmedType = type.trim()
        if (trimmedName.isEmpty() || trimmedType.isEmpty()) {
            throw IllegalArgumentException("이름과 종류를 입력하세요.")
        }
        if (age < 0) {
            throw IllegalArgumentException("나이는 0 이상이어야 합니다.")
        }
        return trimmedName to trimmedType
    }

    // 반려동물과 연결된 일정, 수행 기록, 건강 기록을 먼저 지운다.
    @Transactional
    fun deletePet(userId: Long, petId: Long) {
        val pet = petRepository.findById(petId)
            .orElseThrow { IllegalArgumentException("pet not found: $petId") }

        if (pet.user?.id != userId) {
            throw IllegalArgumentException("unauthorized: pet does not belong to user")
        }

        deletePetGraph(petId)
    }

    @Transactional
    fun deletePet(petId: Long) {
        if (!petRepository.existsById(petId)) {
            throw IllegalArgumentException("pet not found: $petId")
        }
        deletePetGraph(petId)
    }

    private fun deletePetGraph(petId: Long) {
        healthRecordRepository.findByPetIdOrderByRecordDateDesc(petId)
            .forEach { healthRecordRepository.delete(it) }
        taskRepository.findByPetIdOrderByIdDesc(petId).forEach { task ->
            task.id?.let { occurrenceRepository.deleteByTaskId(it) }
            taskRepository.delete(task)
        }
        petRepository.deleteById(petId)
    }
}
